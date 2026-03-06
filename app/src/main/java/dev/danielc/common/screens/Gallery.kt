package dev.danielc.common.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ui.Iconbutton
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.GoGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MimeType {
    FILE,
    FOLDER,
    JPEG,
    PNG,
    MOV,
}

enum class DisplayType {
    THUMBNAILS,
    FILENAME_TABLE,
}

@Suppress("ArrayInDataClass")
data class GalleryObject(
    val isFulfilled: Boolean = false,
    val filename: String? = null,
    val jpegThumb: ByteArray? = null,
    val colorThumb: Color? = null,
    val mimeType: MimeType? = null,
    val createdDate: String? = null,
)

data class GalleryObjectReference(
    val index: Int,
    val isPriority: Boolean,
)

data class GalleryState(
    val displayType: DisplayType = DisplayType.THUMBNAILS,
    val objects: MutableList<GalleryObject> = mutableListOf(),
    val queue: ArrayDeque<GalleryObjectReference> = ArrayDeque()
)

class GalleryViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryState())

    fun reset() {
        viewModelScope.launch() {
            withContext(Dispatchers.Default) {
                _uiState.update { currentState ->
                    currentState.copy(objects = mutableListOf())
                }
            }
        }
    }

    fun setObject(i: Int, obj: GalleryObject) {
        viewModelScope.launch(Dispatchers.Default) {
            val list = _uiState.value.objects.toMutableList()
            list[i] = obj
            _uiState.update { currentState ->
                currentState.copy(objects = list)
            }
        }
    }

    fun enqueueObject(index: Int, isPriority: Boolean = false) {
        _uiState.value.queue.addLast(GalleryObjectReference(index, isPriority))
    }
}

@Composable
fun DrawGalleryObject(obj: GalleryObject) {
    var mod = Modifier.aspectRatio(1f)
    if (obj.colorThumb != null) mod = mod.background(obj.colorThumb)
    Box(modifier = mod.padding(2.dp)) {
        if (obj.filename != null) {
            val icon = when (obj.mimeType) {
                MimeType.FILE -> R.drawable.baseline_question_mark_24
                MimeType.FOLDER -> R.drawable.baseline_folder_open_24
                MimeType.JPEG -> R.drawable.baseline_landscape_24
                MimeType.PNG -> R.drawable.baseline_landscape_24
                MimeType.MOV -> R.drawable.baseline_movie_24
                null -> R.drawable.baseline_landscape_24
            }
            Icon(
                modifier = Modifier.align(Alignment.TopEnd),
                painter = painterResource(icon),
                contentDescription = null,
            )
            Text(obj.filename, modifier = Modifier.align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                style = MaterialTheme.typography.labelSmall
            )
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
            Surface(shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer) {
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

            val listState = rememberLazyListState()

            LazyVerticalGrid(
                columns = GridCells.Fixed(4)
            ) {
                items(state.objects) { obj ->
                    DrawGalleryObject(obj)
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
        GalleryObject(colorThumb = Color.Red, filename = "DSC1111.JPG", mimeType = MimeType.JPEG),
        GalleryObject(colorThumb = Color.Green, filename = "DSC1112.MOV", mimeType = MimeType.MOV),
        GalleryObject(filename = "DCIM/", mimeType = MimeType.FOLDER),
        GalleryObject(colorThumb = Color.Cyan),
        GalleryObject(colorThumb = Color.Magenta),
        GalleryObject(colorThumb = Color.Yellow),
        GalleryObject(colorThumb = Color.Gray),
        GalleryObject(colorThumb = Color.LightGray),
        GalleryObject(colorThumb = Color.DarkGray),
        GalleryObject(colorThumb = Color.Red, filename = "DSC1132.JPG", mimeType = MimeType.JPEG),
        GalleryObject(colorThumb = Color.Green),
        GalleryObject(colorThumb = Color.Blue),
        GalleryObject(colorThumb = Color.Cyan),
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Gallery(navController, innerPadding, state)
        }
    }
}