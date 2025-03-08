package com.android.grafika

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.grafika.ui.component.GrafikaTopAppBar
import com.android.grafika.ui.component.MoreDropdownMenu
import com.android.grafika.ui.theme.GrafikaTheme

class ListSampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GrafikaTheme {
                ListSampleScreen(
                    TESTS, ::startActivity,
                    MORE_ITEMS, ::onMoreItemClick
                )
            }
        }

        ContentManager.initialize(this)

        val cm = ContentManager.getInstance()
        if (!cm.isContentCreated(this)) {
            cm.createAll(this)
        }
    }

    private fun startActivity(listItem: ListItem) {
        val intent = Intent()
        try {
            val cls = Class.forName("com.android.grafika." + listItem.className)
            intent.setClass(this, cls)
            startActivity(intent)
        } catch (cnfe: ClassNotFoundException) {
            throw RuntimeException("Unable to find " + listItem.className, cnfe)
        }
    }

    private fun onMoreItemClick(item: String) {
        when (item) {
            "Regenerate content" -> {
                ContentManager.getInstance().createAll(this)
            }
            "About" -> {
                AboutBox.display(this)
            }
            else ->  {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ListSampleScreenPreview() {
    GrafikaTheme {
        ListSampleScreen(TESTS.take(3), dropdownItems = listOf("Item 1", "Item 2"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSampleScreen(
    items: List<ListItem>,
    onItemClick: (ListItem) -> Unit = {},
    dropdownItems: List<String> = listOf(),
    onMoreItemClick: (String) -> Unit = {},
) {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            GrafikaTopAppBar(
                title = "Grafika",
                actions = {
                    MoreDropdownMenu(
                        itemNames = dropdownItems,
                        onItemClick = onMoreItemClick
                    )
                }
            )
        },
    ) { innerPadding ->
        ItemList(items, innerPadding, onItemClick)
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemListPreview() {
    GrafikaTheme {
        ItemList(TESTS.take(3), PaddingValues(0.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemPreview() {
    GrafikaTheme {
        ListItemView(
            ListItem(
                title = "Play video (TextureView)",
                description = "Plays .mp4 videos created by Grafika",
                className = "PlayMovieActivity",
            )
        )
    }
}


data class ListItem(
    val title: String,
    val description: String,
    val className: String,
)

@Composable
fun ItemList(
    items: List<ListItem>,
    paddingValues: PaddingValues,
    onItemClick: (ListItem) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.padding(paddingValues),
    ) {
        items(items) { item ->
            ListItemView(item) {
                onItemClick(item)
            }
        }
    }
}

@Composable
fun ListItemView(item: ListItem, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = item.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold // Make the title bold
        )
        Text(
            text = item.description,
            fontSize = 16.sp,
//            modifier = Modifier.padding(top = 4.dp)
        )
        HorizontalDivider()
    }
}

private val MORE_ITEMS = listOf(
    "Regenerate content",
    "About"
)

private val TESTS = listOf(
    ListItem(
        "* Play video (TextureView)",
        "Plays .mp4 videos created by Grafika",
        "PlayMovieActivity"
    ),
    ListItem(
        "Continuous capture",
        "Records camera continuously, saves a snapshot when requested",
        "ContinuousCaptureActivity"
    ),
    ListItem("Double decode", "Decodes two videos side-by-side", "DoubleDecodeActivity"),
    ListItem(
        "Hardware scaler exerciser",
        "Exercises SurfaceHolder#setFixedSize()",
        "HardwareScalerActivity"
    ),
    ListItem(
        "Live camera (TextureView)",
        "Trivially feeds the camera preview to a view",
        "LiveCameraActivity"
    ),
    ListItem(
        "Multi-surface test",
        "Three overlapping SurfaceViews, one secure",
        "MultiSurfaceActivity"
    ),
    ListItem(
        "Play video (SurfaceView)",
        "Plays .mp4 videos created by Grafika",
        "PlayMovieSurfaceActivity"
    ),
    ListItem(
        "Record GL app",
        "Records GL app with FBO, re-render, or FB blit",
        "RecordFBOActivity"
    ),
    ListItem(
        "Record screen using MediaProjectionManager",
        "Screen recording using MediaProjectionManager and Virtual Display",
        "ScreenRecordActivity"
    ),
    ListItem("Scheduled swap", "Exercises SurfaceFlinger PTS handling", "ScheduledSwapActivity"),
    ListItem(
        "Show + capture camera",
        "Shows camera preview, records when requested",
        "CameraCaptureActivity"
    ),
    ListItem(
        "Simple GL in TextureView",
        "Renders with GL as quickly as possible",
        "TextureViewGLActivity"
    ),
    ListItem(
        "Simple Canvas in TextureView",
        "Renders with Canvas as quickly as possible",
        "TextureViewCanvasActivity"
    ),
    ListItem(
        "Texture from Camera",
        "Resize and zoom the camera preview",
        "TextureFromCameraActivity"
    ),
    ListItem(
        "{bench} glReadPixels speed test",
        "Tests glReadPixels() performance with 720p frames",
        "ReadPixelsActivity"
    ),
    ListItem(
        "{bench} glTexImage2D speed test",
        "Tests glTexImage2D() performance on 512x512 image",
        "TextureUploadActivity"
    ),
    ListItem("{util} Color bars", "Shows RGB color bars", "ColorBarActivity"),
    ListItem("{util} OpenGL ES info", "Dumps info about graphics drivers", "GlesInfoActivity"),
    ListItem("{~ignore} Chor test", "Exercises bug", "ChorTestActivity"),
    ListItem("{~ignore} Codec open test", "Exercises bug", "CodecOpenActivity"),
    ListItem("{~ignore} Software input surface", "Exercises bug", "SoftInputSurfaceActivity"),
)