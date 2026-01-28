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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.Widgets
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.GoGreen

enum class MimeType {
    FILE,
    FOLDER,
    JPEG,
    PNG,
    MOV,
}

@Suppress("ArrayInDataClass")
data class GalleryObject(
    val filename: String? = null,
    val jpegThumb: ByteArray? = null,
    val colorThumb: Color? = null,
    val mimeType: MimeType? = null,
    val createdDate: String? = null,
)

data class GalleryState(
    val objects: List<GalleryObject>
)

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
            Text(obj.filename, modifier = Modifier.align(Alignment.BottomStart)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
fun GalleryMenu(navController: NavHostController, innerPadding: PaddingValues, state: GalleryState) {
    Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Widgets.Iconbutton(
                    modifier = Modifier.size(50.dp),
                    onClick = {

                    },
                ) {
                    Icon(
                        tint = MaterialTheme.colorScheme.onPrimary,
                        painter = painterResource(R.drawable.baseline_grid_view_24),
                        contentDescription = "Grid View"
                    )
                }
                Widgets.Iconbutton(
                    modifier = Modifier.size(50.dp),
                    onClick = {},
                ) {
                    Icon(
                        tint = MaterialTheme.colorScheme.onPrimary,
                        painter = painterResource(R.drawable.baseline_view_list_24),
                        contentDescription = "List View"
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4)
            ) {
                items(state.objects) { obj ->
                    DrawGalleryObject(obj)
                }
            }
        }
        LinearProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            color = GoGreen,
            progress = { 57f }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun GalleryScreen(navController: NavHostController = rememberNavController()) {
    val state = GalleryState(listOf(
        GalleryObject(colorThumb = Color.Red, filename = "DSC1111.JPG", mimeType = MimeType.JPEG),
        GalleryObject(colorThumb = Color.Green, filename = "DSC1112.MOV", mimeType = MimeType.MOV),
        GalleryObject(colorThumb = Color.Blue),
        GalleryObject(colorThumb = Color.Cyan),
        GalleryObject(colorThumb = Color.Magenta),
        GalleryObject(colorThumb = Color.Yellow),
        GalleryObject(colorThumb = Color.Gray),
        GalleryObject(colorThumb = Color.LightGray),
        GalleryObject(colorThumb = Color.DarkGray),
        GalleryObject(colorThumb = Color.Red),
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
            GalleryMenu(navController, innerPadding, state)
        }
    }
}