package com.android.grafika

import android.content.Intent
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.android.grafika.transcode.ExtractDecodeEditEncodeMuxVideo
import com.android.grafika.ui.theme.GrafikaTheme
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors

class TranscodeVideoActivity : ComponentActivity() {
    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GrafikaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    TopScreenSection(innerPadding, ::onInputSelected, ::onRequestTranscode)
                }
            }
        }
    }

    private fun onRequestTranscode()  {
        if (selectedUri != null) {
            Timber.i("Start config output video")
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
                    Timber.i("Start transcode video")
                    ExtractDecodeEditEncodeMuxVideo.TestWrapper.runTest(extractDecodeEditEncodeMuxVideo)
                }
                    .onSuccess {
                        Timber.i("Transcode success")
                        isTranscodeSuccess = true
                    }

                    .onFailure {
                        Timber.e(it)
                        isTranscodeSuccess = false
                    }


                if (isTranscodeSuccess) {
                    runOnUiThread {
                        val uri = File(outputFileName).toUri()
                        val intent = Intent(this@TranscodeVideoActivity, PlayMovieGLSurfaceActivity::class.java)
                        intent.putExtra("source", uri)
                        startActivity(intent)
                    }
                }
            }
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