/** Bridge between non-android Kotlin code and native C runtime code
 * (allows kotlin code to be used through compose multiplatform) */
package dev.danielc.fudge

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import androidx.room.Room
import dev.danielc.common.AppDatabase
import dev.danielc.common.FileMetadata
import dev.danielc.common.ModuleInstance
import dev.danielc.common.Runtime
import dev.danielc.common.getMimeType
import dev.danielc.libpak.Exif
import dev.danielc.libpak.Pak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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
        if (imageHorizontalSize != null && imageHorizontalSize > GL10.GL_MAX_TEXTURE_SIZE) {
            options.inSampleSize = 2
            options.inDensity = 2
            options.inTargetDensity = 2
            options.inScaled = true
        }

        try {
            var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

            if (orientation != null) {
                val matrix = Matrix()
                matrix.postRotate(orientation.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true)
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

    fun readFile(path: String): ByteArray? {
        if (path.startsWith("file:///android_asset/")) {
            val assman = Pak.getActivity().assets
            val f = assman.open(path.substringAfter("file:///android_asset/"))
            return f.readBytes()
        } else {
            val f = File(path)
            if (!f.exists()) return null
            return f.readBytes()
        }
    }

    fun shareFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(Pak.getActivity().contentResolver, "Shared Image", uri)
        }
        val chooserIntent = Intent.createChooser(intent, "Share")
        Pak.getActivity().startActivity(chooserIntent)
    }

    fun openImageInDefaultApp(file: MediaStoreFile) {
        shareFile(file.contentUri)
    }

    fun openImageInDefaultApp(filename: String) {
        shareFile("file://${filename}".toUri())
    }

    fun filesFromDirectory(path: String): List<String> {
        val dir = File(path)
        val files = dir.listFiles() ?: throw Exception("Error listing files in directory")
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

    fun scanImage(path: String) {
        MediaScannerConnection.scanFile(Pak.getActivity(), arrayOf(path), null, null)
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
        val metadata: FileMetadata,
    )

    fun readFile(file: MediaStoreFile): ByteArray? {
        val resolver = Pak.getActivity().contentResolver
        val fd = resolver.openFileDescriptor(file.contentUri, "r") ?: return null
        val stream = FileInputStream(fd.fileDescriptor)
        val data = stream.readBytes()
        stream.close()
        fd.close()
        return data
    }

    fun writeImageFile(filename: String, data: ByteArray) {
        val resolver = Pak.getActivity().contentResolver

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + File.separator + "fudge")
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return
        val stream = resolver.openOutputStream(uri) ?: return
        stream.write(data)
        stream.close()
    }

    fun getMediaThumbnail(file: MediaStoreFile): ImageBitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bitmap = Pak.getActivity().contentResolver.loadThumbnail(file.contentUri, Size(640, 480), null)
            return bitmap.asImageBitmap()
        } else {
            val thumb = Exif.getExifThumbnail(file.path) ?: return null
            return decodeImageContents(thumb, null)
        }
    }

    fun requestExternalImagesPermission() {
        val ctx = Pak.getActivity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ctx.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                Handler(ctx.mainLooper).post {
                    ctx.requestPermissions(arrayOf<String?>(Manifest.permission.READ_MEDIA_IMAGES), 1)
                }
            }
        }
    }

    fun getInternalDataDirectory(): String {
        return Pak.getActivity().filesDir.path
    }

    fun getDefaultDownloadDirectory(): String {
        val mainStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
        val path = mainStorage + File.separator + "fudge"
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return path
    }

    fun getFiles(subfolder: String = "fudge"): List<MediaStoreFile> {
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
                MediaStore.Images.Media.ORIENTATION,
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
            val orientationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ORIENTATION)
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
                        orientation = cursor.getInt(orientationColumn),
                    )
                )
            }
        }
        return list
    }
}