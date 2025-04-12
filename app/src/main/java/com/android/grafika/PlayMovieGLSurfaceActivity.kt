package com.android.grafika

import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.grafika.player.MoviePlayerV2.PlayTask
import com.android.grafika.databinding.ActivityPlayMovieGlsurfaceBinding
import com.android.grafika.gles.EglCore
import com.android.grafika.gles.FrameRect
import com.android.grafika.gles.Texture2dProgram
import com.android.grafika.gles.WindowSurface
import com.android.grafika.player.MoviePlayerV2
import com.android.grafika.ui.theme.GrafikaTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PlayMovieGLSurfaceActivity : ComponentActivity(),
    SurfaceHolder.Callback,
    MoviePlayerV2.PlayerFeedback,
    GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener{

    private var playTask: MoviePlayerV2.PlayTask? = null

    private lateinit var binding: ActivityPlayMovieGlsurfaceBinding

    private val _playbackStateFlow = MutableStateFlow(VideoPlaybackState.EmptyInput)
    private val playbackStateFlow: Flow<VideoPlaybackState> = _playbackStateFlow

    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityPlayMovieGlsurfaceBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val movieFiles = MiscUtils.getFiles(filesDir, "*.mp4")
        binding.glSurfaceView.setEGLContextClientVersion(3)
        binding.glSurfaceView.setRenderer(this)
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.Default)
            setContent {
                GrafikaTheme {

                    val playbackState by playbackStateFlow.collectAsStateWithLifecycle(initialValue = VideoPlaybackState.EmptyInput)

                    TopScreenSection(
                        playbackState,
                        movieFiles.toList(),
                        onInputSelected = ::onInputSelected,
                        onControlPlaybackRequest = ::onControlPlaybackRequest,
                    )
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sourceUri = intent?.let { IntentCompat.getParcelableExtra(it, "source", Uri::class.java) }
        Timber.i("Source uri from intent: $sourceUri")
        if (sourceUri != null) {
            onInputSelected(sourceUri.toString())
        }
    }

    fun onInputSelected(filePath: String) {
        if (filePath.isNotEmpty()) {
            _playbackStateFlow.value = VideoPlaybackState.InputReady

            selectedUri = if (filePath.startsWith("content://") || filePath.startsWith("file://")) {
                filePath.toUri()
            } else {
                File(filesDir, filePath).toUri()
            }

            Timber.i("Selected uri: $selectedUri")
        }
    }

    fun onControlPlaybackRequest(playbackRequestState: PlaybackRequestState) {
        fun updateUI() {
            _playbackStateFlow.value = when (playbackRequestState) {
                PlaybackRequestState.Play -> VideoPlaybackState.Playing
                PlaybackRequestState.Stop -> VideoPlaybackState.Stopped
            }
        }

        when (playbackRequestState) {
            PlaybackRequestState.Stop ->  {
                stopPlayback()

                // Don't update the UI here - let the task thread do it after the movie has
                // actually stopped.
                // updateUI()
            }

            PlaybackRequestState.Play -> {
                if (playTask == null) {

                    val callback = com.android.grafika.player.SpeedControlCallback()

                    surfaceTexture!!.setOnFrameAvailableListener(this)

                    var player: MoviePlayerV2? = null
                    try {
                        player = MoviePlayerV2(contentResolver, selectedUri!!, surface!!, callback)
                    } catch (ioe: IOException) {
                        surface!!.release()
                        return
                    }

                    fullScreen!!.updateInfo(
                        player.videoWidth, player.videoHeight, player.videoOrientation,
                        frameWidth, frameHeight
                    )

                    playTask = PlayTask(player, this)
                    updateUI()
                    playTask?.execute()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.glSurfaceView.onPause()

        if (playTask != null) {
            stopPlayback()
            playTask?.waitForStop()
        }
    }

    private fun stopPlayback() {
        playTask?.requestStop()
    }


    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {}


    override fun playbackStopped() {
        playTask = null
        _playbackStateFlow.value = VideoPlaybackState.Stopped
    }


    /**
     * Clears the playback surface to black.
     */
    private fun clearSurface(surface: Surface) {
        // We need to do this with OpenGL ES (*not* Canvas -- the "software render" bits
        // are sticky).  We can't stay connected to the Surface after we're done because
        // that'd prevent the video encoder from attaching.
        //
        // If the Surface is resized to be larger, the new portions will be black, so
        // clearing to something other than black may look weird unless we do the clear
        // post-resize.
        val eglCore = EglCore()
        val win = WindowSurface(eglCore, surface, false)
        win.makeCurrent()
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        win.swapBuffers()
        win.release()
        eglCore.release()
    }

    private var fullScreen: FrameRect? = null
    private var textureId: Int = -1
    private var surfaceTexture: SurfaceTexture? = null
    private var stMatrix = FloatArray(16)
    private var surface: Surface? = null

    private var frameWidth: Int = 0
    private var frameHeight: Int = 0

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        fullScreen = FrameRect(
            Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT_CLAMP_TO_BORDER)
        )

        textureId = fullScreen!!.createTextureObject()
        surfaceTexture = SurfaceTexture(textureId)
        surface = Surface(surfaceTexture)
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        this.frameHeight = height
        this.frameWidth = width

        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(stMatrix)
        fullScreen?.drawFrame(textureId, stMatrix)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        binding.glSurfaceView.requestRender()
    }

}

@Preview(showBackground = true)
@Composable
fun TopScreenSectionPreview() {
    GrafikaTheme {

        TopScreenSection(
            presetItems = listOf("Option 1", "Option 2", "Option 3")
        )
    }
}


@Composable
fun PresetInputDialog(
    radioOptions: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
) {
    Column {
        radioOptions.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .selectable(
                        selected = (option == selectedOption),
                        onClick = {
                            onOptionSelected(option)
                        }
                    )
                    .fillMaxWidth(),
            ) {
                RadioButton(
                    selected = (option == selectedOption),
                    onClick = {
                        onOptionSelected(option)
                    }
                )
                Text(
                    text = option,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun TopScreenSection(
    videoPlaybackState: VideoPlaybackState = VideoPlaybackState.EmptyInput,
    presetItems: List<String> = emptyList(),
    onInputSelected: (String) -> Unit = {},
    onControlPlaybackRequest: (PlaybackRequestState) -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    val pickedUriOrPath = remember { mutableStateOf<String>("") }

    val pickVideoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            pickedUriOrPath.value = uri.toString()
            onInputSelected.invoke(uri.toString())
        }


    if (showDialog) {
        var selectedOption by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose preset input") },
            text = {
                PresetInputDialog(
                    radioOptions = presetItems,
                    selectedOption = selectedOption,
                    onOptionSelected = { selectedOption = it }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false

                        selectedOption?.let {
                            pickedUriOrPath.value = it
                            onInputSelected.invoke(it)
                        }
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
    Column {
        Row {
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Preset input")
            }

            Button(
                onClick = { pickVideoLauncher.launch("video/*") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Local file")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = {
                    when (videoPlaybackState) {
                        VideoPlaybackState.Playing -> onControlPlaybackRequest.invoke(PlaybackRequestState.Stop)
                        else -> onControlPlaybackRequest.invoke(PlaybackRequestState.Play)
                    }
                },
                enabled = videoPlaybackState != VideoPlaybackState.EmptyInput,
            ) {
                Icon(
                    if (videoPlaybackState == VideoPlaybackState.Playing) Icons.Filled.Stop
                    else Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                )
            }

            Text(text = pickedUriOrPath.value)
        }
    }
}

enum class VideoPlaybackState {
    EmptyInput,
    InputReady,
    Playing,
    Stopped,
}

enum class PlaybackRequestState {
    Play,
    Stop,
}
