package dev.danielc.fudge

import android.annotation.SuppressLint
import android.app.ActionBar
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer

fun renderText(text: String, fg: Int, bg: Int): ByteArray {
    val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas: Canvas = Canvas(bitmap)
    bitmap.eraseColor(bg or -0x1000000)
    val textPaint: Paint = Paint()
    textPaint.setTextSize(32f)
    textPaint.setAntiAlias(true)
    textPaint.setColor(fg or -0x1000000)
    canvas.drawText(text, 10f, 40f, textPaint)

    val byteBuffer = ByteBuffer.allocate(bitmap.getByteCount())
    bitmap.copyPixelsToBuffer(byteBuffer)
    byteBuffer.rewind()
    return byteBuffer.array()
}

class Liveview : SurfaceHolder.Callback {
    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        val surfaceHolder = surfaceHolder.surface

        val canvas: Canvas = surfaceHolder.lockCanvas(Rect(0, 0, 100, 100))
        val p = Paint()
        p.setColor(Color.RED)
        //canvas.drawColor(Color.BLACK);
        canvas.drawRect(0f, 0f, 100f, 100f, p)
        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    override fun surfaceChanged(holder: SurfaceHolder, i2: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }
}

@Composable
fun FramebufferSurface(modifier: Modifier = Modifier, haveHandle: (Any) -> Unit = {}) {
    AndroidView(modifier = modifier, factory = { ctx ->
        val view = SurfaceView(ctx)
        haveHandle(view.holder as Any)
        view
    }, update = { view ->

    })
}