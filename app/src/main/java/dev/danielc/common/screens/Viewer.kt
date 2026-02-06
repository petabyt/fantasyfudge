package dev.danielc.common.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.Widgets
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.GoGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        painter = painterResource(R.drawable.background),
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