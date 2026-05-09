package dev.danielc.common.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielc.R
import dev.danielc.common.FileHandle
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.fudge.AndroidRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocalGalleryViewModel(val directory: String, val viewer: ViewerModel) : GalleryViewModel() {
    var files = emptyList<AndroidRuntime.MediaStoreFile>()
    init {
        CoroutineScope(Dispatchers.IO).launch {
            files = AndroidRuntime.getFiles()
            setProperties(files.size, directory, SortBy.NEWEST_FIRST)
        }
        start()
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }

    override fun fulfillThumbnail(file: GalleryObjectReference) {
        updateThumbnail(file.index, AndroidRuntime.getMediaThumbnail(files[file.index]))
    }

    override fun fulfillMetadata(file: GalleryObjectReference) {
        updateMetadata(file.index, files[file.index].metadata)
    }

    fun loadImage(i: Int) {
        val file = files[i]
        viewer.update(FileHandle(i), files.size)
        viewer.updateMetadata(file.metadata)
        viewer.updateStats(10,"Reading file")
        val data = AndroidRuntime.readFile(file)
        if (data == null) {
            viewer.setError("Failed to decode image")
        } else {
            viewer.setFileContents(data, false)
        }

        viewer.updateSideBitmaps(
            if (files.getOrNull(i - 1) == null) null else AndroidRuntime.getMediaThumbnail(files[i - 1]),
            if (files.getOrNull(i + 1) == null) null else AndroidRuntime.getMediaThumbnail(files[i + 1])
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalGallery(onBack: () -> Unit, onItemClick: (Int) -> Unit, modifier: Modifier, model: LocalGalleryViewModel?) {
    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("Local Files")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            onBack()
                        }) {
                            Icon(painterResource(R.drawable.outline_arrow_back_24), contentDescription = null)
                        }
                    },
                )
            },
        ) { innerPadding ->
            if (model != null) {
                val state by model.uiState.collectAsStateWithLifecycle()
                Gallery(modifier.padding(innerPadding), state, requestLoad = { i ->
                    model.enqueueObject(i, true)
                }, onItemClick = { i ->
                    onItemClick(i)
                })
            }
        }
    }
}