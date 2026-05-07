/** Bridge between non-android Kotlin code and native C runtime code
 * (allows kotlin code to be used through compose multiplatform) */
package dev.danielc.fudge

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import dev.danielc.common.Exif
import dev.danielc.common.FileMetadata
import dev.danielc.common.ModuleManifest
import dev.danielc.common.NativeModule
import dev.danielc.common.Runtime
import dev.danielc.common.getMimeType
import dev.danielc.libpak.Pak
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    external fun setupGoveeLifeModule(mod: NativeModule?, manifest: ModuleManifest?): Int
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

    fun openImageInDefaultApp(filename: String) {
        val intent = Intent()
        intent.setAction(Intent.ACTION_VIEW)
        intent.setDataAndType(("file://${filename}").toUri(), "image/*")
        weakCtx?.get()?.startActivity(intent)
    }

    fun filesFromDirectory(path: String): List<String> {
        val dir: File = File(path)
        val files = dir.listFiles()
        if (files == null) throw Exception("Error listing files in directory")
        val list = mutableListOf<String>()
        for (e in files) list.add(e.path)
        return list
    }

    fun getDownloadDirectory(): String {
        val mainStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
        val fujifilm = mainStorage + File.separator + "fudge"
        val directory = File(fujifilm)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return fujifilm
    }

    fun scanImage(path: String?) {
        MediaScannerConnection.scanFile(Pak.getActivity(), arrayOf<String?>(path), null, null)
    }

    fun writeFile(data: ByteArray, filename: String) {
        val path = getDownloadDirectory() + File.separator + filename
        val file = File(path)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            fos.write(data)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (fos != null) {
                try {
                    fos.close()
                    this.scanImage(path)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    data class MediaStoreFile(
        val contentUri: Uri,
        val path: String,
        val metadata: FileMetadata
    )

    fun getMediaThumbnail(file: MediaStoreFile): ImageBitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bitmap = Pak.getActivity().contentResolver.loadThumbnail(file.contentUri, Size(640, 480), null)
            return bitmap.asImageBitmap()
        } else {
            val thumb = Exif.getExifThumbnail(file.path)
            if (thumb == null) return null;
            return decodeImageContents(thumb, null)
        }
    }

    fun getFiles(subfolder: String = "fudge"): List<MediaStoreFile> {
//        val ctx = Pak.getActivity()
//        if (ctx.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
//            Handler(ctx.mainLooper).post { ctx.requestPermissions(arrayOf<String?>(Manifest.permission.READ_MEDIA_IMAGES), 1) }
//        }

        val list = mutableListOf<MediaStoreFile>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        Pak.getActivity().contentResolver.query(
            collection,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.MIME_TYPE,
            ),
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("Pictures/${subfolder}/%"),
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                list += MediaStoreFile(
                    ContentUris.withAppendedId(collection, id),
                    cursor.getString(dataColumn),
                    FileMetadata(
                        filename = cursor.getString(nameColumn),
                        mimeType = getMimeType(cursor.getString(typeColumn)),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        filesize = cursor.getInt(sizeColumn),
                    )
                )
            }
        }
        return list
    }

    @JvmStatic
    fun logGlobalLine(s: String) {
        Log.d("logGlobalLine", s)
        Runtime.mainLog.addLine(s)
    }
}