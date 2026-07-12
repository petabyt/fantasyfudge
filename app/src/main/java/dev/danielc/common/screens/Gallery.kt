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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.FileHandle
import dev.danielc.common.FileMetadata
import dev.danielc.common.ui.theme.FudgeRippleConfig
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.fudge.AndroidRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

private fun bitmapFromColor(
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
    var lastChecked: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow(),
    var invalidMetadata: Boolean = false,
    var invalidThumbnail: Boolean = false,
)

data class GalleryObjectReference(
    val index: Int,
    val isPriority: Boolean,
)

data class FilesystemState(
    val storageName: String? = null,
    val userSortBy: SortBy = SortBy.NEWEST_FIRST,
    val displayType: DisplayType = DisplayType.THUMBNAILS,
    val objectListSortedOrder: SortBy = SortBy.NEWEST_FIRST,
    // object is null if it hasn't been loaded/checked yet
    val objects: List<GalleryObject?> = emptyList(),
    val queue: ArrayDeque<GalleryObjectReference> = ArrayDeque()
)

abstract class GalleryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FilesystemState())
    val uiState = _uiState.asStateFlow()
    private var thread: Job? = null
    private var threadIsPaused: Boolean = true
    private var isThumbnailPriority: Boolean = true
    private val queueMutex = Mutex()

    override fun onCleared() {
        stop()
    }

    fun getMetadata(file: FileHandle): FileMetadata? {
        return _uiState.value.objects.getOrNull(file.index)?.metadata
    }
    fun getThumbnail(file: FileHandle, offset: Int): ImageBitmap? {
        return _uiState.value.objects.getOrNull(file.index + offset)?.thumbnail
    }

    open fun onRefresh() {
        // ...
    }
    open fun onShare(ref: GalleryObjectReference) {
        // ...
    }
    open fun itemClicked(ref: GalleryObjectReference) {
        // ...
    }
    open fun init() {
        // ...
    }
    abstract fun fulfillThumbnail(file: GalleryObjectReference)
    abstract fun fulfillMetadata(file: GalleryObjectReference)

    private fun checkObject(obj: GalleryObject?, ref: GalleryObjectReference, doThumbnail: Boolean = true): Boolean {
        if (obj == null) {
            if (isThumbnailPriority && doThumbnail) {
                fulfillThumbnail(ref)
            } else {
                fulfillMetadata(ref)
            }
        } else {
            obj.lastChecked = TimeSource.Monotonic.markNow()
            if (obj.metadata == null && !obj.invalidMetadata) {
                fulfillMetadata(ref)
            } else if (obj.thumbnail == null && !obj.invalidThumbnail && doThumbnail) {
                fulfillThumbnail(ref)
            } else {
                return false
            }
        }
        return true
    }

    // Returns true when work was done
    private suspend fun tick(): Boolean {
        val queue = _uiState.value.queue
        val objects = _uiState.value.objects
        if (queue.isEmpty()) {
            // If queue is empty iterate all non-null objects and fulfill them
            for (i in objects.indices) {
                if (objects[i] != null &&  checkObject(objects[i], GalleryObjectReference(i, true), doThumbnail = true)) {
                    return true
                }
            }
            return false
        }
        val ref = queueMutex.withLock {
            queue.removeLast()
        }
        if (ref.index >= objects.size || ref.index < 0) return false
        val obj = objects[ref.index]
        if (obj != null) {
            return checkObject(obj, ref)
        } else {
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
                        delay(100)
                    } catch (_: CancellationException) {
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                currentState.copy(objects = mutableListOf())
            }
        }
    }

    fun setProperties(nItems: Int, name: String, sortBy: SortBy) {
        viewModelScope.launch(Dispatchers.IO) {
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

    private fun updateObject(i: Int, block: (GalleryObject) -> GalleryObject) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                val list = currentState.objects.toMutableList()
                while (list.size <= i) list.add(null)
                val obj = list[i] ?: GalleryObject()
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
    fun enqueueObjects(indexes: List<Int>, isPriority: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            queueMutex.withLock {
                println("enqueueing ${indexes.size}")
                for (i in indexes) {
                    _uiState.value.queue.removeIf { it.index == i }
                    _uiState.value.queue.addLast(GalleryObjectReference(i, isPriority))
                }
            }
        }
    }
    fun trimMemory(nObjectsToFree: Int = 10) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                val list = currentState.objects.toMutableList()
                val oldest = _uiState.value.objects.sortedByDescending { it?.lastChecked }
                var nFreed = 0
                for (e in oldest) {
                    if (e != null && e.thumbnail == null) {
                        val index = list.indexOf(e)
                        list[index] = list[index]?.copy(thumbnail = null)
                        nFreed++
                    }
                    if (nFreed >= nObjectsToFree) break else nFreed++
                }
                currentState.copy(objects = list)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryThumbnail(obj: GalleryObject?, onClick: () -> Unit = {}) {
    val boxModifier = Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceContainer)
    CompositionLocalProvider(LocalRippleConfiguration provides FudgeRippleConfig(Color.White)) {
        Box(
            boxModifier
            .combinedClickable(
                onClick = {
                    onClick()
                },
                onLongClick = {
                    onClick()
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
                if (obj.thumbnail == null || obj.metadata?.mimeType == MimeType.MOV) {
                    Icon(
                        modifier = Modifier.align(Alignment.Center).size(45.dp),
                        painter = painterResource(icon),
                        contentDescription = null,
                    )
                }
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

fun longToFileSize(bytes: Long): String {
    if (bytes <= 0) return "0b"

    val units = arrayOf("b", "kb", "mb", "gb", "tb", "pb", "eb")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "${size.toInt()} ${units[unitIndex]}"
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
                    Text(longToFileSize(obj.metadata.filesize.toLong()))
                }
            }
        }
    }
}

// TODO:
data class GalleryCallbacks(
    val onRefresh: () -> Unit = {},
    val requestLoad: (Int) -> Unit = {},
    val onItemClick: (Int) -> Unit = {},
    val setDisplayType: (DisplayType) -> Unit = {},
)

@Composable
fun GalleryWithModel(onItemClick: (Int) -> Unit, modifier: Modifier, model: GalleryViewModel?) {
    if (model != null) {
        val state by model.uiState.collectAsStateWithLifecycle()
        Gallery(modifier, state, requestLoad = { items ->
            model.enqueueObjects(items, true)
        }, onItemClick = { i ->
            onItemClick(i)
        }, onRefresh = {
            model.onRefresh()
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gallery(modifier: Modifier = Modifier, state: FilesystemState, requestLoad: (List<Int>) -> Unit = {}, onItemClick: (Int) -> Unit = {}, onRefresh: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }
    var displayType by rememberSaveable { mutableStateOf(DisplayType.THUMBNAILS) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var rows by rememberSaveable { mutableIntStateOf(4) }
    @Composable
    fun menu() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().padding(2.dp),
        ) {
//            Box(Modifier.padding(4.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))) {
//                Text("sdcard", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(6.dp))
//            }
            Spacer(Modifier.weight(1f))
            val interactionSource = remember { MutableInteractionSource() }
            Slider(modifier = Modifier.weight(1f), value = rows.toFloat(), valueRange = 2f..5f , steps = 3, onValueChange = {
                rows = it.toInt()
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }, thumb = {
                SliderDefaults.Thumb(interactionSource, thumbSize = DpSize(25.dp, 25.dp))
            })
            IconButton(onClick = {
                displayType = if (displayType == DisplayType.THUMBNAILS) DisplayType.VERTICAL_TABLE else DisplayType.THUMBNAILS
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }, modifier = Modifier) {
                Icon(
                    modifier = Modifier.size(27.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                    painter = if (displayType == DisplayType.THUMBNAILS) painterResource(R.drawable.baseline_view_list_24) else painterResource(R.drawable.baseline_grid_view_24),
                    contentDescription = null
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
            val rows = if (displayType == DisplayType.THUMBNAILS) rows else 1
            LazyVerticalGrid(
                state = listState,
                columns = GridCells.Fixed(rows)
            ) {
                item(span = { GridItemSpan(rows) }) {
                    menu()
                }
                if (state.objects.isNotEmpty()) {
                    itemsIndexed(state.objects) { index, obj ->
                        if (displayType == DisplayType.THUMBNAILS) {
                            GalleryThumbnail(obj, onClick = {
                                onItemClick(index)
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            })
                        } else {
                            GalleryFile(obj, onClick = {
                                onItemClick(index)
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            })
                        }
                    }
                } else {
                    item(span = { GridItemSpan(rows) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("No files are present.")
                        }
                    }
                }
            }

            // Monitor recently viewed items so they can be sent to the queue
            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
                    .distinctUntilChanged()
                    .collect { visibleItems ->
                        requestLoad(visibleItems)
                    }
            }
        }
        if (false) {
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(8.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Column {
                    Text("DSC123.JPG")
                    Text("123x831")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun PreviewGalleryScreen(navController: NavHostController = rememberNavController()) {
    val state = FilesystemState(objects = mutableListOf(
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