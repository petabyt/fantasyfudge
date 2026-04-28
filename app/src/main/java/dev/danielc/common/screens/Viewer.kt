package dev.danielc.common.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.FileHandle
import dev.danielc.common.FileMetadata
import dev.danielc.common.Runtime
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

data class ViewerState(
    val handle: FileHandle,
    val numberOfItems: Int,
    val metadata: FileMetadata? = null,
    val isLoading: Boolean = true,
    val isDecoding: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val currentDownloadProgress: Int = 0,
    val currentDownloadSpeed: String = "",
    val painter: Painter? = null,
    val bitmap: ImageBitmap? = null,
)

class ViewerModel() : ViewModel() {
    private val _viewerState = MutableStateFlow<ViewerState?>(null)
    val viewerState = _viewerState.asStateFlow()

    fun update(file: FileHandle, numberOfItems: Int) {
        _viewerState.value = ViewerState(file, numberOfItems)
    }
    fun updateMetadata(metadata: FileMetadata?) {
        _viewerState.update { viewerState ->
            viewerState?.copy(
                metadata = metadata
            )
        }
    }
    fun clear() {
        _viewerState.value = null
    }
    fun setFileContents(data: ByteArray) {
        _viewerState.update { viewerState ->
            viewerState?.copy(
                isDecoding = true
            )
        }
        val bitmap = Runtime.decodeImageContents(data, null)
        if (bitmap == null) {
            setError("Failed to decode image contents")
        } else {
            _viewerState.update { viewerState ->
                viewerState?.copy(
                    bitmap = bitmap,
                    isDecoding = false,
                    isLoading = false,
                )
            }
        }
    }
    fun update(downloadPercent: Int, downloadSpeed: String) {
        _viewerState.update { viewerState ->
            viewerState?.copy(
                currentDownloadProgress = downloadPercent,
                currentDownloadSpeed = downloadSpeed,
            )
        }
    }
    fun setError(message: String) {
        _viewerState.update { viewerState ->
            viewerState?.copy(
                isError = true,
                errorMessage = message
            )
        }
    }
}

@Composable
fun Viewer(modifier: Modifier = Modifier, state: ViewerState, switchTo: (Int) -> Unit, close: () -> Unit) {
    val filename = state.metadata?.filename ?: "File"

    if (state.isError) {
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
                    Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Error",
                            style = TextStyle(
                                fontSize = 20.sp
                            ),
                        )
                        Text("'${state.errorMessage.orEmpty()}'", color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            close()
                        }) {
                            Text("Exit")
                        }
                    }
                }
            }
        }
        return
    } else
    if (state.isLoading) {
        val text = "Downloading " + when (state.metadata?.mimeType) {
            MimeType.JPEG, MimeType.PNG -> "image"
            MimeType.MOV -> "movie"
            else -> "file"
        }

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
                            progress = { state.currentDownloadProgress.toFloat() / 100 }
                        )
                        Text(
                            text = state.currentDownloadSpeed,
                            modifier = Modifier,
                        )
                        Button(onClick = {
                            close()
                        }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
        return
    }

    val painter = state.painter ?: painterResource(R.drawable.baseline_photo_camera_24)

    val imageYOffset = remember {
        Animatable(0f)
    }
    val scaleFactor = remember {
        Animatable(1f)
    }

    // Swipe image box down to exit the viewer screen
    val scope = rememberCoroutineScope()
    val screenHeightDp = LocalWindowInfo.current.containerSize.height
    val minOffsetToClose = screenHeightDp / 10
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
                if (imageYOffset.value >= 100) {
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
        val pagerState = rememberPagerState(initialPage = state.handle.index, pageCount = {
            state.numberOfItems
        })
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                if (page != state.handle.index) switchTo(page)
            }
        }
        HorizontalPager(
            state = pagerState, modifier = Modifier.fillMaxSize().align(Alignment.Center)) { page ->
            if (page == state.handle.index) {
                val zoomState = rememberZoomState(contentSize = painter.intrinsicSize)
                if (state.bitmap != null) {
                    Image(
                        modifier = Modifier.align(Alignment.Center).zoomable(zoomState),
                        bitmap = state.bitmap,
                        contentDescription = filename,
                    )
                } else {
                    Image(
                        modifier = Modifier.align(Alignment.Center).zoomable(zoomState),
                        painter = painter,
                        contentDescription = filename,
                    )
                }
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
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(ViewerState(
        handle = FileHandle(index = 10, "sdcard"),
        metadata = FileMetadata("DSCF00001.JPG", mimeType = MimeType.JPEG),
        painter = painter,
        isLoading = false,
        currentDownloadSpeed = "5 mbps",
        currentDownloadProgress = 40,
        numberOfItems = 30,
        isError = true,
        errorMessage = "Failed to decode",
    )) }
    ViewerScreen(state, switchTo = { i ->
        state = state.copy(
            currentDownloadProgress = 0,
            isLoading = true
        )
        scope.launch {
            while (state.currentDownloadProgress < 100) {
                state = state.copy(currentDownloadProgress = state.currentDownloadProgress + 1)
                delay(1)
            }
            state = state.copy(isLoading = false)
        }
    }, close = {
        navController.navigateUp()
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(state: ViewerState, switchTo: (Int) -> Unit, close: () -> Unit) {
    return FudgeTheme {
        BackHandler {
            close()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text(state.metadata?.filename ?: "File")
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