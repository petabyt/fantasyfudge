package dev.danielc.common.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.FileMetadata
import dev.danielc.common.Runtime
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.FudgeRippleConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

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
    val colorThumb: Int? = null,
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
    val objects: List<GalleryObject?> = emptyList(),
    val queue: ArrayDeque<GalleryObjectReference> = ArrayDeque()
)

class GalleryViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryState())
    val uiState = _uiState.asStateFlow()

    fun reset() {
        viewModelScope.launch() {
            withContext(Dispatchers.Default) {
                _uiState.update { currentState ->
                    currentState.copy(objects = mutableListOf())
                }
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

    fun setListLength(size: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { currentState ->
                val list = currentState.objects.toMutableList()
                while (list.size < size) list.add(null)
                currentState.copy(objects = list)
            }
        }
    }

    fun updateObject(i: Int, md: FileMetadata? = null, thumbData: ByteArray? = null) {
        // TODO: randomly firing twice
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { currentState ->
                val list = currentState.objects.toMutableList()
                while (list.size <= i) list.add(null)
                var obj = list[i] ?: GalleryObject(md)
                if (thumbData != null) {
                    try {
                        obj = obj.copy(thumbnail = Runtime.decodeImageContents(thumbData, null))
                    } catch (e: Exception) {
                        println(e.message)
                        // TODO: decode error
                    }
                }
                if (md != null) {
                    obj = obj.copy(metadata = md)
                }

                list[i] = obj
                currentState.copy(objects = list)
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

    val backgroundColor = if (obj != null && obj.colorThumb != null) {
        Color(0xff000000 or obj.colorThumb.toLong())
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val icon = if (obj == null) {
        R.drawable.baseline_question_mark_24
    } else {
        when (obj.metadata?.mimeType) {
            null, MimeType.FILE -> R.drawable.baseline_question_mark_24
            MimeType.FOLDER -> R.drawable.baseline_folder_open_24
            MimeType.JPEG -> R.drawable.baseline_landscape_24
            MimeType.PNG -> R.drawable.baseline_landscape_24
            MimeType.MOV -> R.drawable.baseline_movie_24
        }
    }

    boxModifier = boxModifier.background(backgroundColor)

    CompositionLocalProvider(LocalRippleConfiguration provides FudgeRippleConfig(Color.White)) {
        Box(
            boxModifier
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
        ) {
            if (obj == null) {
                Icon(
                    modifier = Modifier.align(Alignment.Center).size(35.dp),
                    painter = painterResource(icon),
                    contentDescription = null,
                )
            } else {
                if (obj.thumbnail != null) {
                    Image(modifier = Modifier.fillMaxSize(), bitmap = obj.thumbnail!!, contentDescription = null)
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
fun Gallery(navController: NavHostController, innerPadding: PaddingValues, state: GalleryState, requestLoad: (Int) -> Unit = {}) {
    Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        Column {
            if (false) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(2.dp),
                    ) {
                        val iconButtonColors = IconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                        )

                        IconButton(
                            colors = iconButtonColors,
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
                            colors = iconButtonColors,
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

            val listState = rememberLazyListState()

            if (state.displayType == DisplayType.THUMBNAILS) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4)
                ) {
                    items(state.objects) { obj ->
                        GalleryThumbnail(obj)
                    }
                }
            } else if (state.displayType == DisplayType.VERTICAL_TABLE) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1)
                ) {
                    items(state.objects) { obj ->
                        GalleryFile(obj)
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun PreviewGalleryScreen(navController: NavHostController = rememberNavController()) {
    val state = GalleryState(objects = mutableListOf(
        GalleryObject(FileMetadata("DCIM/", mimeType = MimeType.FOLDER)),
        GalleryObject(FileMetadata("DSC1111.JPG", mimeType = MimeType.JPEG), colorThumb = Color.Red.toArgb()),
        GalleryObject(FileMetadata("DSC1234.MOV", mimeType = MimeType.MOV), colorThumb = Color.Green.toArgb()),
        GalleryObject(FileMetadata(), colorThumb = Color.Cyan.toArgb()),
        GalleryObject(FileMetadata(), colorThumb = Color.Magenta.toArgb()),
        GalleryObject(FileMetadata(), colorThumb = Color.Yellow.toArgb()),
        null,
        GalleryObject(FileMetadata(), colorThumb = Color.Gray.toArgb()),
        GalleryObject(FileMetadata(), colorThumb = Color.LightGray.toArgb()),
        GalleryObject(FileMetadata(), colorThumb = Color.DarkGray.toArgb()),
        GalleryObject(FileMetadata("DSC1132.JPG"), colorThumb = Color.Red.toArgb()),
        GalleryObject(FileMetadata(), colorThumb = Color.Green.toArgb()),
        GalleryObject(FileMetadata(), colorThumb = Color.Blue.toArgb()),
        GalleryObject(FileMetadata(), colorThumb = Color.Cyan.toArgb()),
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
            Gallery(navController, innerPadding, state)
        }
    }
}