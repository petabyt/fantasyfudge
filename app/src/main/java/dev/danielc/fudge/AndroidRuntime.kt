/** Bridge between non-android Kotlin code and native C runtime code
 * (allows kotlin code to be used through compose multiplatform) */
package dev.danielc.fudge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.room.Room
import dev.danielc.common.AppDatabase
import dev.danielc.common.ModuleInstance
import dev.danielc.common.Runtime
import dev.danielc.libpak.Pak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.microedition.khronos.opengles.GL10

object AndroidRuntime {
    var hasInited: Boolean = false
    private var databaseInstance: AppDatabase? = null
    fun setup(ctx: Context) {
        databaseInstance = Room.databaseBuilder(
            ctx,
            AppDatabase::class.java,
            "app_database"
        )
        .fallbackToDestructiveMigration(true)
        .build()
        init()
    }
    fun getDatabase(): AppDatabase {
        return databaseInstance!!
    }
    fun getDatabaseNullable(): AppDatabase? {
        return databaseInstance
    }
    fun resetDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            getDatabase().clearAllTables()
        }
    }

    external fun init()
    external fun setupSharedLibraryModule(mod: ModuleInstance, path: String): Int
    external fun setupJavascriptModule(mod: ModuleInstance, fileContents: ByteArray): Int
    external fun setupWebassemblyModule(mod: ModuleInstance, fileContents: ByteArray): Int

    @JvmStatic
    fun logGlobalLine(s: String) {
        Log.d("global-log", s)
        Runtime.logGlobalLine(s)
    }

    @JvmStatic
    fun getDeviceFriendlyName(): String {
        val deviceName = Settings.Global.getString(Pak.getActivity().contentResolver, "device_name")

        if (!deviceName.isNullOrBlank()) {
            return deviceName.replace(" ", "-") + "-fudge"
        }

        return "${Build.MANUFACTURER}-${Build.MODEL}" + "-fudge"
    }

    fun decodeImageContents(data: ByteArray, imageHorizontalSize: Int? = null, orientation: Int? = null): ImageBitmap? {
        val options = BitmapFactory.Options()

//        if (imageHorizontalSize != null && imageHorizontalSize > GL10.GL_MAX_TEXTURE_SIZE) {
//            options.inSampleSize = 2
//            options.inDensity = 2
//            options.inTargetDensity = 2
//            options.inScaled = true
//        }

        try {
            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            if (bitmap.width > GL10.GL_MAX_TEXTURE_SIZE) {
                bitmap = null
                System.gc()
                // TODO: do math
                options.inSampleSize = 2
                options.inDensity = 2
                options.inTargetDensity = 2
                options.inScaled = true
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            }

            if (orientation != null) {
                val matrix = Matrix()
                matrix.postRotate(orientation.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            return bitmap.asImageBitmap()
        } catch (e: Exception) {
            return null
        }
    }

    fun getJsonManifestList(): List<String> {
        val assman = Pak.getActivity().assets
        try {
            val files = ArrayList<String>()
            val list = assman.list("") ?: return mutableListOf()
            for (s in list) {
                if (!s.endsWith(".json")) continue
                files.add("file:///android_asset/${s}")
            }
            return files
        } catch (e: Exception) {
            Log.e("NR", e.toString())
            return mutableListOf()
        }
    }
}