package dev.danielc.common.screens

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.GoGreen
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

data class ViewerState(
    val type: MimeType = MimeType.FILE,
    val filename: String? = null,
    val isLoading: Boolean = true,
    val currentDownloadProgress: Int = 0,
    val currentDownloadSpeed: String = "",
    val painter: Painter? = null,
    val numberOfItems: Int = 100,
    var indexInItems: Int = 5,
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
fun Viewer(modifier: Modifier = Modifier, state: ViewerState, switchTo: (Int) -> Unit, close: () -> Unit) {
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

    val imageYOffset = remember {
        Animatable(0f)
    }
    val scaleFactor = remember {
        Animatable(1f)
    }

    val scope = rememberCoroutineScope()
    val screenHeightDp = LocalWindowInfo.current.containerSize.height
    val minOffsetToClose = screenHeightDp / 8
    val swipeToCloseGesture = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { _, dragAmount ->
                scope.launch {
                    if ((imageYOffset.value + dragAmount.y) >= 0) {
                        launch {
                            imageYOffset.animateTo(imageYOffset.value + dragAmount.y)
                        }
                        launch {
                            scaleFactor.animateTo(
                                (1f - (imageYOffset.value / 1000f)).coerceIn(
                                    0.5f,
                                    1f
                                )
                            )
                        }
                    }
                }
            },
            onDragEnd = {
                if (imageYOffset.value >= minOffsetToClose) {
                    close()
                } else {
                    scope.launch {
                        launch {
                            imageYOffset.animateTo(0f)
                        }
                        launch {
                            scaleFactor.animateTo(1f)
                        }
                    }
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()
        .graphicsLayer {
            scaleX = scaleFactor.value
            scaleY = scaleFactor.value
        }
        .offset(0.dp, imageYOffset.value.dp)
        .then(swipeToCloseGesture)
    ) {
        val pagerState = rememberPagerState(initialPage = state.indexInItems, pageCount = {
            state.numberOfItems
        })
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                if (page != state.indexInItems) switchTo(page)
            }
        }
        HorizontalPager(
            state = pagerState, modifier = Modifier.fillMaxSize().align(Alignment.Center)) { page ->
            if (page == state.indexInItems) {
                println("render image")
                val zoomState = rememberZoomState(contentSize = painter.intrinsicSize)
                Image(
                    modifier = Modifier.align(Alignment.Center).zoomable(zoomState),
                    painter = painter,
                    contentDescription = filename,
                )
            } else {
                Image(
                    modifier = Modifier.align(Alignment.Center),
                    painter = painterResource(R.drawable.baseline_question_mark_24),
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun PreviewViewer(navController: NavController = rememberNavController()) {
    val painter = painterResource(R.drawable.image)
    var state by remember { mutableStateOf(ViewerState(
        filename = "DSCF0001.JPG",
        painter = painter,
        isLoading = false,
        type = MimeType.JPEG,
        currentDownloadSpeed = "5 mbps",
        currentDownloadProgress = 40,

        indexInItems = 12,
        numberOfItems = 30,
    )) }
    ViewerScreen(state, switchTo = { i ->
        println("switching to ${i}")
        state = state.copy(
            filename = "DSCF0002.JPG",
            indexInItems = i,
            isLoading = true
        )
    }, close = {
        navController.navigateUp()
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(state: ViewerState, switchTo: (Int) -> Unit, close: () -> Unit) {
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
                            close()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.outline_arrow_back_24),
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
            Viewer(Modifier.padding(innerPadding), state,
                switchTo = switchTo,
                close = {
                    close()
                }
            )
        }
    }
}