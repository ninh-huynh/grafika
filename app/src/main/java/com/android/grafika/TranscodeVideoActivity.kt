package com.android.grafika

import android.content.Intent
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.DefaultVideoFrameProcessor
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultAssetLoaderFactory
import androidx.media3.transformer.DefaultDecoderFactory
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.DefaultMuxer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Transformer.ProgressState
import androidx.media3.transformer.VideoEncoderSettings
import com.android.grafika.media3.transcode.MyCustomAssetLoader
import com.android.grafika.transcode.ExtractDecodeEditEncodeMuxVideo
import com.android.grafika.ui.theme.GrafikaTheme
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors

class TranscodeVideoActivity : ComponentActivity() {
    private var selectedUri: Uri? = null

    private var useCTSSolution = false
    private var useMedia3TransformerSolution = true
    private val TAG = TranscodeVideoActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val onRequestTranscode: () -> Unit = if (useCTSSolution) {
            ::onRequestTranscodeByCTS
        } else if (useMedia3TransformerSolution) {
            ::onRequestTranscodeMedia3Transformer
        } else {
            {}
        }

        setContent {
            GrafikaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    TopScreenSection(innerPadding, ::onInputSelected, onRequestTranscode)
                }
            }
        }
    }

    private fun onRequestTranscodeByCTS() {
        if (selectedUri != null) {
            Timber.tag(TAG).i("Start config output video")
            val extractDecodeEditEncodeMuxVideo = ExtractDecodeEditEncodeMuxVideo()

            extractDecodeEditEncodeMuxVideo.setSize(1280, 720)
            extractDecodeEditEncodeMuxVideo.setSource(selectedUri)
            extractDecodeEditEncodeMuxVideo.setCopyVideo()
            extractDecodeEditEncodeMuxVideo.setOutputVideoMimeType(MediaFormat.MIMETYPE_VIDEO_AVC)
            extractDecodeEditEncodeMuxVideo.setContentResolver(contentResolver)
            extractDecodeEditEncodeMuxVideo.setOutputDir(filesDir)

            val outputFileName = extractDecodeEditEncodeMuxVideo.generateOutputFile()
            extractDecodeEditEncodeMuxVideo.setOutputFile(outputFileName)

            Executors.newSingleThreadExecutor().execute {
                var isTranscodeSuccess: Boolean = false
                runCatching {
                    Timber.tag(TAG).i("Start transcode video")
                    ExtractDecodeEditEncodeMuxVideo.TestWrapper.runTest(extractDecodeEditEncodeMuxVideo)
                }
                    .onSuccess {
                        Timber.tag(TAG).i("Transcode success")
                        isTranscodeSuccess = true
                    }

                    .onFailure {
                        Timber.tag(TAG).e(it)
                        isTranscodeSuccess = false
                    }


                if (isTranscodeSuccess) {
                    runOnUiThread {
                        val uri = File(outputFileName).toUri()
                        playOutputVideo(uri)
                    }
                }
            }
        }
    }

    private fun playOutputVideo(uri: Uri) {
        val intent = Intent(this@TranscodeVideoActivity, PlayMovieGLSurfaceActivity::class.java)
        intent.putExtra("source", uri)
        startActivity(intent)
    }

    @OptIn(UnstableApi::class)
    private fun onRequestTranscodeMedia3Transformer() {
        if (selectedUri != null) {
            val uiHandler = Handler(this.mainLooper)
            val outputFile = File(
                filesDir, "media3-transformer/%d_video_720p.mp4".format(
                    System.currentTimeMillis()
                )
            )

            val transformerListener: Transformer.Listener =
                object : Transformer.Listener {
                    override fun onCompleted(compsition: Composition, result: ExportResult) {
                        Timber.tag(TAG).i("Transcode success")
                        playOutputVideo(outputFile.toUri())
                    }

                    override fun onError(
                        composition: Composition, result: ExportResult,
                        exception: ExportException
                    ) {
                        Timber.tag(TAG).e(exception)
                    }
                }

            val inputMediaItem = MediaItem.Builder()
                .apply {
                    setUri(selectedUri!!)

//                    setClippingConfiguration(
//                        ClippingConfiguration.Builder()
//                            .setStartPositionMs(10_000)
//                            .setEndPositionMs(20_000)
//                            .build()
//                    )
                }
                .build()

            val editedMediaItem =
                EditedMediaItem.Builder(inputMediaItem)
                    .setRemoveAudio(false)
                    .setEffects(
                        Effects(
                            /* audioProcessors= */ listOf(),
                            /* videoEffects= */ listOf(
                                Presentation.createForHeight(720)
                            )
                        ))
                    .build()

            val transformer = Transformer.Builder(this)
                .experimentalSetTrimOptimizationEnabled(false)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(transformerListener)
                .setEncoderFactory(
                    DefaultEncoderFactory.Builder(this)
                        .setEnableFallback(true)
                        .setRequestedVideoEncoderSettings(
                            VideoEncoderSettings.Builder().apply {
                                setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                                setBitrate(VideoEncoderSettings.NO_VALUE)
                                setiFrameIntervalSeconds(VideoEncoderSettings.DEFAULT_I_FRAME_INTERVAL_SECONDS)
                                setEncodingProfileLevel(
                                    VideoEncoderSettings.NO_VALUE,
                                    VideoEncoderSettings.NO_VALUE
                                )
                            }
                                .build()
                        )
                        .build()
                )
                .setAssetLoaderFactory(
                    MyCustomAssetLoader.Factory(this)
                )
                .setVideoFrameProcessorFactory(
                    DefaultVideoFrameProcessor.Factory.Builder()
                        .build()
                )
                .setMuxerFactory(
                    DefaultMuxer.Factory()  // FrameworkMuxer
                )
                .build()

            val composition = Composition.Builder(EditedMediaItemSequence.Builder(editedMediaItem).build())
                .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
                .build()

            transformer.start(composition, outputFile.absolutePath)
            Timber.tag(TAG).i("Start transcoding async...")

            val progressHolder = ProgressHolder()
            uiHandler.post(
                object : Runnable {
                    override fun run() {
                        val progressState: @ProgressState Int = transformer.getProgress(progressHolder)
                        Timber.tag(TAG).i("Transcoding %d ...", progressHolder.progress)
                        if (progressState != Transformer.PROGRESS_STATE_NOT_STARTED) {
                            uiHandler.postDelayed(/* r= */this,  /* delayMillis= */500)
                        }
                    }
                }
            )
        }
    }

    private fun onInputSelected(filePath: String) {
        if (filePath.isNotEmpty()) {
            selectedUri = if (filePath.startsWith("content://")) {
                filePath.toUri()
            } else {
                File(filePath).toUri()
            }
            Timber.i("Selected uri: $selectedUri")
        }
    }

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GrafikaTheme {
        TopScreenSection()
    }
}

@Composable
fun TopScreenSection(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onInputSelected: (String) -> Unit = {},
    onRequestTranscode: () -> Unit = {},
) {
    val pickedUriOrPath = remember { mutableStateOf<String>("") }

    val pickVideoLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            pickedUriOrPath.value = uri.toString()
            onInputSelected.invoke(uri.toString())
        }


    Column(modifier = Modifier.padding(paddingValues)) {
        Row {

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
                    onRequestTranscode.invoke()
                },
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                )
            }

            Text(text = pickedUriOrPath.value)
        }
    }
}