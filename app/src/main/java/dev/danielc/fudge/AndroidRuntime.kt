/** Bridge between non-android Kotlin code and native C runtime code
 * (allows kotlin code to be used through compose multiplatform) */
package dev.danielc.fudge

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Network
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.danielc.common.ModuleManifest
import dev.danielc.common.NativeModule
import dev.danielc.common.Runtime
import java.lang.ref.WeakReference
import javax.microedition.khronos.opengles.GL10

class WiFiAdapter {
    var net: Network? = null
}

object AndroidRuntime {
    var hasInited: Boolean = false
    var weakCtx: WeakReference<Context>? = null
    fun setupAndroidContext(ctx: Context?) {
        weakCtx = WeakReference<Context>(ctx)
    }

    external fun init()
    external fun setupDummyNativeModule(mod: NativeModule?, manifest: ModuleManifest?): Int
    external fun setupCmfNothingAudioModule(mod: NativeModule?, manifest: ModuleManifest?): Int
    external fun setupLibFujiModule(mod: NativeModule?, manifest: ModuleManifest?): Int
    external fun setupJavascriptModule(
        mod: NativeModule?,
        manifest: ModuleManifest?,
        jsPath: String?
    ): Int
    external fun setupWebassemblyModule(
        mod: NativeModule?,
        manifest: ModuleManifest?,
        wasmPath: String?
    ): Int

    fun decodeImageContents(data: ByteArray, imageHorizontalSize: Int?): ImageBitmap? {
        val options = BitmapFactory.Options()
        if (imageHorizontalSize != null && imageHorizontalSize > GL10.GL_MAX_TEXTURE_SIZE) {
            options.inSampleSize = 2
            options.inDensity = 2
            options.inTargetDensity = 2
            options.inScaled = true
        }

        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
        return bitmap.asImageBitmap()
    }

    fun getJsonManifestList(): List<String> {
        val ctx: Context = weakCtx!!.get()!!
        val assman = ctx.assets
        try {
            val files = ArrayList<String>()
            val list = assman.list("")
            if (list == null) return mutableListOf<String>()
            for (s in list) {
                if (!s.endsWith(".json")) continue
                files.add(s)
            }
            return files
        } catch (e: Exception) {
            Log.e("NR", e.toString())
            return mutableListOf()
        }
    }

    @Throws(Exception::class)
    fun readAssetsFile(path: String): ByteArray {
        val ctx: Context = weakCtx!!.get()!!
        val assman = ctx.assets
        val f = assman.open(path)
        val buffer = ByteArray(f.available())
        val n = f.read(buffer)
        f.close()
        return buffer
    }

    @JvmStatic
    fun logGlobalLine(s: String) {
        Log.d("logGlobalLine", s)
        Runtime.mainLog.addLine(s)
    }
}