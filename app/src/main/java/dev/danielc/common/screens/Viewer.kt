package dev.danielc.common.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.GoGreen
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

data class ViewerState(
    val type: MimeType = MimeType.FILE,
    val filename: String? = null,
    val isLoading: Boolean = true,
    val currentDownloadProgress: Int = 0,
    val currentDownloadSpeed: String = "",
    val painter: Painter? = null,
)

//@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DownloadingDialog(speed: String = "2mb/s", percent: Int = 57, text: String = "Downloading") {
    Dialog(onDismissRequest = {

    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = text,
                        modifier = Modifier,
                        style = TextStyle(
                            fontSize = 20.sp
                        ),
                    )
                    LinearProgressIndicator(
                        modifier = Modifier,
                        color = MaterialTheme.colorScheme.primary,
                        progress = { percent.toFloat() / 100 }
                    )
                    Text(
                        text = speed,
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}

@Composable
fun Viewer(navController: NavHostController, innerPadding: PaddingValues, state: ViewerState) {
    val filename = state.filename ?: "File"

    if (state.isLoading) {
        val text = "Downloading " + when (state.type) {
            MimeType.JPEG, MimeType.PNG -> "image"
            MimeType.MOV -> "movie"
            else -> "file"
        }
        DownloadingDialog(state.currentDownloadSpeed, state.currentDownloadProgress, text)
        return
    }

    val painter = state.painter ?: painterResource(R.drawable.baseline_photo_camera_24)

    Box(modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
    ) {
        val zoomState = rememberZoomState(contentSize = painter.intrinsicSize)
        Image(
            modifier = Modifier.align(Alignment.Center).zoomable(zoomState),
            painter = painter,
            contentDescription = filename,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun PreviewViewer(navController: NavHostController = rememberNavController()) {
    val state = ViewerState(
        filename = "DSCF0001.JPG",
        painter = painterResource(R.drawable.image),
        isLoading = false,
        type = MimeType.JPEG,
        currentDownloadSpeed = "5 mbps",
        currentDownloadProgress = 40,
    )

    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text(state.filename ?: "File")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigateUp()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                painter = painterResource(R.drawable.outline_save_24),
                                contentDescription = "Save"
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_share_24),
                                contentDescription = "Share"
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Viewer(navController, innerPadding, state)
        }
    }
}