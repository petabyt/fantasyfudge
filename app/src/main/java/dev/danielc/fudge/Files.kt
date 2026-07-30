package dev.danielc.fudge

import android.Manifest
import android.content.ClipData
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import dev.danielc.common.FileMetadata
import dev.danielc.common.screens.MimeType
import dev.danielc.fudge.AndroidRuntime.decodeImageContents
import dev.danielc.libpak.Exif
import dev.danielc.libpak.Pak
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileLayer {
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

    private fun shareFile(uri: Uri) {
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

    data class Handle(
        val fd: ParcelFileDescriptor,
        val uri: Uri,
        val write: Boolean = true
    ) {
        val streamIn: InputStream? = if (write) FileInputStream(fd.fileDescriptor) else null
        val streamOut: OutputStream? = if (!write) FileOutputStream(fd.fileDescriptor) else null
        fun write(byteArray: ByteArray) {
            streamOut?.write(byteArray)
        }
        fun close() {
            streamOut?.close()
            streamIn?.close()
            fd.close()
        }
    }

    fun readFile(file: MediaStoreFile): ByteArray? {
        val resolver = Pak.getActivity().contentResolver
        val fd = resolver.openFileDescriptor(file.contentUri, "r") ?: return null
        val stream = FileInputStream(fd.fileDescriptor)
        val data = stream.readBytes()
        stream.close()
        fd.close()
        return data
    }

    fun openFileForReading(ref: MediaStoreFile): Handle? {
        val resolver = Pak.getActivity().contentResolver
        return Handle(resolver.openFileDescriptor(ref.contentUri, "r") ?: return null, ref.contentUri)
    }

    fun openFileForWriting(filename: String, metadata: FileMetadata): Handle? {
        val resolver = Pak.getActivity().contentResolver

        val pair = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (MimeType.fromString(metadata.mimeType).isImage()) {
                Pair(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), Environment.DIRECTORY_PICTURES)
            } else {
                Pair(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), Environment.DIRECTORY_MOVIES)
            }
        } else {
            if (MimeType.fromString(metadata.mimeType).isVideo()) {
                Pair(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_PICTURES)
            } else {
                Pair(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MOVIES)
            }
        }
        val collection = pair.first
        val directory = pair.second

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, metadata.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, directory + File.separator + "fudge")
        }

        val uri = resolver.insert(collection, values) ?: return null
        return Handle(resolver.openFileDescriptor(uri, "r") ?: return null, uri)
    }

    fun getMediaThumbnail(file: MediaStoreFile): ImageBitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val bitmap = Pak.getActivity().contentResolver.loadThumbnail(file.contentUri, Size(640, 480), null)
                return bitmap.asImageBitmap()
            } catch (ignored: Exception) {
                println("${ignored.message} ${file.contentUri}")
                return null
            }
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

    fun getDownloadedMediaFiles(subfolder: String = "fudge"): List<MediaStoreFile> {
        val list = mutableListOf<MediaStoreFile>()
        val columns = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.ORIENTATION,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )

        val selection = (
            "("
            + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
            + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            + " OR "
            + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
            + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            + ")"
            + " AND "
            + "("
            + "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
            + " OR "
            + "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
            + ")"
        )

        Pak.getActivity().contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            columns,
            selection,
            arrayOf("Pictures/${subfolder}%", "Movies/${subfolder}%"),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val orientationColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.ORIENTATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val type = cursor.getInt(typeColumn)
                val collection = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                }

                list += MediaStoreFile(
                    collection,
                    cursor.getString(dataColumn),
                    FileMetadata(
                        filename = cursor.getString(nameColumn),
                        mimeType = cursor.getString(mimeTypeColumn),
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