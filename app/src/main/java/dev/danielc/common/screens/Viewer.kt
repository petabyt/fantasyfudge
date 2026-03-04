package dev.danielc.common.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ui.theme.FudgeTheme
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

data class ViewerState(
    val filename: String? = null,
    val isLoading: Boolean = true,
    val painter: Painter? = null,
)

@Composable
fun Viewer(navController: NavHostController, innerPadding: PaddingValues, state: ViewerState) {
    val filename = state.filename ?: "File"

    val painter = if (state.isLoading) {
        painterResource(R.drawable.outline_tools_power_drill_24)
    } else if (state.painter != null) {
        state.painter
    } else {
        painterResource(R.drawable.baseline_photo_camera_24)
    }

    Box(modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                Text("Loading...")
            }
        } else {
            val zoomState = rememberZoomState(contentSize = painter.intrinsicSize)
            Image(
                modifier = Modifier.align(Alignment.Center).zoomable(zoomState),
                painter = painter,
                contentDescription = filename,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun PreviewViewer(navController: NavHostController = rememberNavController()) {
    val state = ViewerState(
        filename = "DSCF0001.JPG",
        painter = painterResource(R.drawable.image),
        isLoading = false
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