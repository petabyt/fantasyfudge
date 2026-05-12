package dev.danielc.common.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.FileHandle
import dev.danielc.common.FileMetadata
import dev.danielc.common.ui.theme.FudgeRippleConfig
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.primaryIconButtonColors
import dev.danielc.fudge.AndroidRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

fun bitmapFromColor(
    color: Color,
    width: Int = 512,
    height: Int = 400
): ImageBitmap {
    val imageBitmap = ImageBitmap(width, height)
    val drawScope = CanvasDrawScope()
    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(imageBitmap),
        size = Size(width.toFloat(), height.toFloat())
    ) {
        drawRect(color = color)
    }

    return imageBitmap
}

enum class MimeType {
    FILE,
    FOLDER,
    JPEG,
    PNG,
    MOV,
}

enum class DisplayType {
    THUMBNAILS,
    VERTICAL_TABLE,
}

enum class SortBy(val id: Int) {
    DEFAULT(0),
    NEWEST_FIRST(1),
    OLDEST_FIRST(2),
    LARGEST_FIRST(3),
    SMALLEST_FIRST(4);

    companion object {
        fun fromId(id: Int): SortBy? {
            return entries.find { it.id == id }
        }
    }
}

data class GalleryObject(
    val metadata: FileMetadata? = null,
    val thumbnail: ImageBitmap? = null,
    var invalidMetadata: Boolean = false,
    var invalidThumbnail: Boolean = false,
)

data class GalleryObjectReference(
    val index: Int,
    val isPriority: Boolean,
)

data class GalleryState(
    val storageName: String? = null,
    val userSortBy: SortBy = SortBy.NEWEST_FIRST,
    val displayType: DisplayType = DisplayType.THUMBNAILS,
    val objectListSortedOrder: SortBy = SortBy.NEWEST_FIRST,
    // TODO: Tree of objects, maintain current directory
    // object if null if it hasn't been loaded/checked yet
    val objects: List<GalleryObject?> = emptyList(),
    val queue: ArrayDeque<GalleryObjectReference> = ArrayDeque()
)

abstract class GalleryViewModel(val requestThumbnails: Boolean = true) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryState())
    val uiState = _uiState.asStateFlow()
    var thread: Job? = null
    var threadIsPaused: Boolean = true
    var isThumbnailPriority: Boolean = true

    fun getMetadata(file: FileHandle): FileMetadata? {
        return _uiState.value.objects.getOrNull(file.index)?.metadata
    }
    fun getThumbnail(file: FileHandle, offset: Int): ImageBitmap? {
        return _uiState.value.objects.getOrNull(file.index + offset)?.thumbnail
    }

    abstract fun fulfillThumbnail(file: GalleryObjectReference)
    abstract fun fulfillMetadata(file: GalleryObjectReference)

    fun checkObject(obj: GalleryObject?, ref: GalleryObjectReference): Boolean {
        if (obj == null) {
            if (isThumbnailPriority) {
                fulfillThumbnail(ref)
            } else {
                fulfillMetadata(ref)
            }
        } else {
            if (obj.metadata == null && !obj.invalidMetadata) {
                fulfillMetadata(ref)
            } else if (obj.thumbnail == null && !obj.invalidThumbnail) {
                fulfillThumbnail(ref)
            } else {
                return false
            }
        }
        return true
    }

    fun tick(): Boolean {
        val queue = _uiState.value.queue
        val objects = _uiState.value.objects
        if (queue.isEmpty()) {
            // If queue is empty, iterate all objects and check for any work to do
            for (i in objects.indices) {
                if (checkObject(objects[i], GalleryObjectReference(i, true))) {
                    return true
                }
            }
            return false
        }
        val ref = queue.removeLast()
        try {
            val obj = _uiState.value.objects[ref.index]
            return checkObject(obj, ref)
        } catch (e: Exception) {
            println(e.message)
            return false
        }
    }

    fun start() {
        threadIsPaused = false
        if (thread == null) {
            thread = CoroutineScope(Dispatchers.IO).launch {
                val thread = thread
                while (thread != null && !thread.isCancelled) {
                    if (!threadIsPaused) {
                        if (tick()) continue
                    }
                    try {
                        delay(10)
                    } catch (e: CancellationException) {
                        break
                    }
                }
            }
        }
    }

    fun setPaused(v: Boolean) {
        threadIsPaused = v
    }

    fun stop() {
        setPaused(true)
        thread?.cancel()
        thread = null
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { currentState ->
                currentState.copy(objects = mutableListOf())
            }
        }
    }

    fun setProperties(nItems: Int, name: String, sortBy: SortBy) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { currentState ->
                val list = currentState.objects.toMutableList()
                while (list.size < nItems) list.add(null)
                currentState.copy(
                    objects = list,
                    storageName = name,
                    objectListSortedOrder = sortBy,
                )
            }
        }
    }

    fun updateObject(i: Int, block: (GalleryObject) -> GalleryObject) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                val list = currentState.objects.toMutableList()
                while (list.size <= i) list.add(null)
                val obj = list[i] ?: GalleryObject(null)
                list[i] = block(obj)
                currentState.copy(objects = list)
            }
        }
    }

    fun updateThumbnail(i: Int, thumbData: ByteArray? = null) {
        updateThumbnail(i, if (thumbData != null) AndroidRuntime.decodeImageContents(thumbData, null) else null)
    }
    fun updateMetadata(i: Int, md: FileMetadata? = null) {
        updateObject(i) { obj ->
            if (md == null) {
                obj.copy(metadata = null, invalidMetadata = true)
            } else {
                obj.copy(metadata = md)
            }
        }
    }
    fun updateThumbnail(i: Int, thumb: ImageBitmap? = null) {
        updateObject(i) { obj ->
            if (thumb == null) {
                obj.copy(
                    thumbnail = null,
                    invalidThumbnail = true,
                )
            } else {
                obj.copy(thumbnail = thumb)
            }
        }
    }
    fun enqueueObject(index: Int, isPriority: Boolean = false) {
        _uiState.value.queue.addLast(GalleryObjectReference(index, isPriority))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryThumbnail(obj: GalleryObject?, onClick: () -> Unit = {}) {
    var boxModifier = Modifier.aspectRatio(1f)

    boxModifier = boxModifier.background(MaterialTheme.colorScheme.surfaceContainer)

    CompositionLocalProvider(LocalRippleConfiguration provides FudgeRippleConfig(Color.White)) {
        Box(
            boxModifier
            .combinedClickable(
                onClick = {
                    onClick()
                },
                onLongClick = {
                    onClick
                }
            )
            .indication(
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() }
            )
        ) {
            if (obj != null) {
                val icon = when (obj.metadata?.mimeType) {
                    null -> R.drawable.baseline_question_mark_24
                    MimeType.FILE -> R.drawable.outline_files_24
                    MimeType.FOLDER -> R.drawable.baseline_folder_open_24
                    MimeType.JPEG -> R.drawable.baseline_landscape_24
                    MimeType.PNG -> R.drawable.baseline_landscape_24
                    MimeType.MOV -> R.drawable.baseline_movie_24
                }

                if (obj.thumbnail != null) {
                    // TODO: Better content scale handling?
                    Image(modifier = Modifier.fillMaxSize(), bitmap = obj.thumbnail, contentDescription = null, contentScale = ContentScale.FillHeight)
                }

                val iconModifier = if (obj.metadata?.mimeType == MimeType.FOLDER) {
                    Modifier.align(Alignment.Center).size(45.dp)
                } else {
                    Modifier.align(Alignment.TopEnd)
                }
                Icon(
                    modifier = iconModifier,
                    painter = painterResource(icon),
                    contentDescription = null,
                )
                if (obj.metadata?.filename != null) {
                    Text(
                        obj.metadata.filename!!, modifier = Modifier.align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryFile(obj: GalleryObject?, onClick: () -> Unit = {}) {
    if (obj == null) return
    CompositionLocalProvider(LocalRippleConfiguration provides FudgeRippleConfig(Color.White)) {
        Box(
            Modifier
                .combinedClickable(
                    onClick = {
                        onClick()
                    },
                    onLongClick = {

                    }
                )
                .indication(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val icon = when (obj.metadata?.mimeType) {
                    null, MimeType.FILE -> R.drawable.baseline_question_mark_24
                    MimeType.FOLDER -> R.drawable.baseline_folder_open_24
                    MimeType.JPEG -> R.drawable.baseline_landscape_24
                    MimeType.PNG -> R.drawable.baseline_landscape_24
                    MimeType.MOV -> R.drawable.baseline_movie_24
                }
                Icon(
                    tint = MaterialTheme.colorScheme.primary,
                    painter = painterResource(icon),
                    contentDescription = null,
                )
                if (obj.metadata?.filename != null) {
                    Text(obj.metadata.filename!!, modifier = Modifier.weight(1f))
                }
                if (obj.metadata?.filesize != null) {
                    Text(obj.metadata.filesize.toString())
                }
            }
        }
    }
}

@Composable
fun Gallery(modifier: Modifier = Modifier, state: GalleryState, requestLoad: (Int) -> Unit = {}, onItemClick: (Int) -> Unit = {}, onRefresh: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    Box(modifier = modifier.fillMaxSize()) {
        Column {
            if (false) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(2.dp),
                    ) {
                        Row(Modifier.weight(1f)) {
                            Box(Modifier.padding(5.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))) {
                                Text("sdcard", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(5.dp))
                            }
                        }

                        IconButton(
                            colors = primaryIconButtonColors(),
                            onClick = {},
                            modifier = Modifier,
                        ) {
                            Icon(
                                tint = MaterialTheme.colorScheme.onPrimary,
                                painter = painterResource(R.drawable.baseline_grid_view_24),
                                contentDescription = "Grid View"
                            )
                        }
                        IconButton(
                            colors = primaryIconButtonColors(),
                            onClick = {},
                            modifier = Modifier,
                        ) {
                            Icon(
                                tint = MaterialTheme.colorScheme.onPrimary,
                                painter = painterResource(R.drawable.baseline_view_list_24),
                                contentDescription = "List View"
                            )
                        }
                    }
                }
            }

            if (state.objects.isNotEmpty()) {
                val listState = rememberLazyGridState()

                PullToRefreshBox(
                    state = rememberPullToRefreshState(),
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        CoroutineScope(Dispatchers.IO).launch {
                            isRefreshing = true
                            onRefresh()
                            refreshTrigger++
                            isRefreshing = false
                        }
                    }
                ) {
                    if (state.displayType == DisplayType.THUMBNAILS) {
                        LazyVerticalGrid(
                            state = listState,
                            columns = GridCells.Fixed(4)
                        ) {
                            itemsIndexed(state.objects) { index, obj ->
                                GalleryThumbnail(obj, onClick = {
                                    onItemClick(index)
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                })
                            }
                        }
                    } else if (state.displayType == DisplayType.VERTICAL_TABLE) {
                        LazyVerticalGrid(
                            state = listState,
                            columns = GridCells.Fixed(1)
                        ) {
                            itemsIndexed(state.objects) { index, obj ->
                                GalleryFile(obj, onClick = {
                                    onItemClick(index)
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                })
                            }
                        }
                    }
                }

                // Monitor recently viewed items so it can be sent to the queue
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                    .collect { visibleItems ->
                        for (e in visibleItems) {
                            requestLoad(e.index)
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.Center) {
                    Text("No files are present.")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun PreviewGalleryScreen(navController: NavHostController = rememberNavController()) {
    val state = GalleryState(objects = mutableListOf(
        GalleryObject(FileMetadata("DCIM/", mimeType = MimeType.FOLDER)),
        GalleryObject(FileMetadata("DSC1111.JPG", mimeType = MimeType.JPEG), thumbnail = bitmapFromColor(Color.Red)),
        GalleryObject(FileMetadata("DSC1234.MOV", mimeType = MimeType.MOV), thumbnail = bitmapFromColor(Color.Green)),
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.Cyan)),
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.Magenta)),
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.Yellow, width = 300)),
        null,
        null,
        null,
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.Gray)),
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.LightGray)),
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.DarkGray, width = 250)),
        GalleryObject(FileMetadata("DSC1132.JPG"), thumbnail = bitmapFromColor(Color.Red)),
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.Green)),
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.Blue)),
        GalleryObject(FileMetadata(), thumbnail = bitmapFromColor(Color.Cyan)),
    ))

    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("Gallery")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigateUp()
                        }) {
                            Icon(painterResource(R.drawable.outline_arrow_back_24), contentDescription = null)
                        }
                    },
                )
            },
        ) { innerPadding ->
            Gallery(Modifier.padding(innerPadding), state)
        }
    }
}