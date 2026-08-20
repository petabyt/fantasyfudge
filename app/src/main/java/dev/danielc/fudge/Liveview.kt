package dev.danielc.fudge

import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.alexvas.rtsp.codec.VideoDecoderSurfaceThread
import com.alexvas.rtsp.widget.RtspProcessor
import com.limelight.binding.video.MediaCodecHelper
import dev.danielc.common.BackgroundViewModel
import dev.danielc.common.ModuleInstance
import dev.danielc.libpak.Pak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import kotlinx.coroutines.runBlocking

class ModuleLiveviewModel(val mod: ModuleInstance): BackgroundViewModel() {
    private var isRtsp: Boolean = false
    private var rtspProcessor: RtspProcessor? = null
    private var currentSurfaceHolder: SurfaceHolder? = null // should be WeakReference?
    private var blockingJob: Job? = null
    fun updateNative(holder: SurfaceHolder) {
        currentSurfaceHolder = holder
        mod.updateNativeLiveview(holder.surface, false)
        val oldJob = blockingJob
        blockingJob = CoroutineScope(Dispatchers.IO).launch {
            oldJob?.join()
            mod.nativeLiveviewThread()
        }
    }
    fun updateRtsp(url: String) {
        isRtsp = true
        rtspProcessor = RtspProcessor(
            onVideoDecoderCreateRequested = {
                    videoMimeType,
                    videoRotation,
                    videoFrameQueue,
                    videoDecoderListener,
                    videoDecoderType,
                    videoFrameRateStabilization,
                ->
                VideoDecoderSurfaceThread(
                    currentSurfaceHolder!!.surface,
                    videoMimeType,
                    1920,
                    1080,
                    videoRotation,
                    videoFrameQueue,
                    videoDecoderListener,
                    videoDecoderType,
                    videoFrameRateStabilization,
                )
            }
        )
        MediaCodecHelper.initialize(Pak.getActivity(), /*glRenderer*/ "")

        rtspProcessor?.init(
            url.toUri(),
            null,
            null,
            null,
            RtspProcessor.DEFAULT_SOCKET_TIMEOUT
        )

        rtspProcessor?.start(requestVideo = true, requestAudio = false, requestApplication = false)
    }
    fun setPaused(v: Boolean) {
        mod.updateNativeLiveview(currentSurfaceHolder?.surface, v)
    }
    suspend fun stopThread() {
        blockingJob?.join()
    }
    fun update(w: Int, h: Int) {
        currentSurfaceHolder?.setFixedSize(720, 480)
    }
    fun clear() {
        rtspProcessor?.stopDecoders()
        rtspProcessor?.stop()
        isRtsp = false
    }
}

class MySurfaceHolderCallback(val model: ModuleLiveviewModel) : SurfaceHolder.Callback {
    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        model.updateNative(surfaceHolder)
    }
    override fun surfaceChanged(holder: SurfaceHolder, i2: Int, width: Int, height: Int) {
        // ...
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("lv", "surfaceDestroyed")
        model.setPaused(true)
        runBlocking {
            model.stopThread()
        }
    }
}

@Composable
fun FramebufferSurface(modifier: Modifier = Modifier, model: ModuleLiveviewModel) {
    AndroidView(modifier = modifier, factory = { ctx ->
        val view = SurfaceView(ctx)
        view.holder.addCallback(MySurfaceHolderCallback(model))
        view
    }, update = { view ->

    })
}