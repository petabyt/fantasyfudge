package dev.danielc.common.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielc.common.FileHandle
import dev.danielc.fudge.AndroidRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocalGalleryViewModel(val directory: String, val viewer: ViewerModel) : GalleryViewModel() {
    var files = emptyList<AndroidRuntime.MediaStoreFile>()
    init {
        CoroutineScope(Dispatchers.IO).launch {
            refresh()
            start()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }

    fun refresh() {
        try {
            files = AndroidRuntime.getFiles()
            reset()
            setProperties(files.size, directory, SortBy.NEWEST_FIRST)
            for (i in files.indices) {
                updateMetadata(i, files[i].metadata)
            }
            enqueueObjects((0..100).toList())
        } catch (e: Exception) {
            // ..
        }
    }

    override fun fulfillThumbnail(file: GalleryObjectReference) {
        updateThumbnail(file.index, AndroidRuntime.getMediaThumbnail(files[file.index]))
    }

    override fun fulfillMetadata(file: GalleryObjectReference) {
        updateMetadata(file.index, files[file.index].metadata)
    }

    override fun itemClicked(ref: GalleryObjectReference) {
        val file = files[ref.index]
        viewer.update(FileHandle(ref.index), files.size)
        viewer.updateMetadata(file.metadata)
        viewer.updateStats(10,"Reading image")
        val data = AndroidRuntime.readFile(file)
        viewer.updateStats(60,"Decoding image")
        if (data == null) {
            viewer.setError("Failed to decode image")
        } else {
            viewer.setFileContents(data, 0, 0)
        }

        viewer.updateSideBitmaps(
            if (files.getOrNull(ref.index - 1) == null) null else AndroidRuntime.getMediaThumbnail(files[ref.index - 1]),
            if (files.getOrNull(ref.index + 1) == null) null else AndroidRuntime.getMediaThumbnail(files[ref.index + 1])
        )
    }

    fun share(i: Int) {
        AndroidRuntime.openImageInDefaultApp(files[i])
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalGallery(onItemClick: (Int) -> Unit, modifier: Modifier, model: LocalGalleryViewModel?) {
    if (model != null) {
        val state by model.uiState.collectAsStateWithLifecycle()
        Gallery(modifier, state, requestLoad = { items ->
            model.enqueueObjects(items)
        }, onItemClick = { i ->
            onItemClick(i)
        }, onRefresh = {
            model.refresh()
        })
    }
}