package com.betelgeuse.corp.examination.documents_build

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DocumentManager(private val context: Context) {

    fun createDocumentFile(title: String): File {
        val fileName = "$title.docx"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(storageDir, fileName)
        return try {
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            if (!file.exists()) {
                file.createNewFile()
            }
            file
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("Ошибка при создании файла: ${e.message}")
        }
    }

    fun saveDocumentToFile(document: XWPFDocument, file: File) {
        FileOutputStream(file).use { out ->
            document.write(out)
        }
    }

    fun shareFile(file: File) {
        val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share File")
        context.startActivity(chooserIntent)
    }
    fun saveDocumentToExternalStorage(document: XWPFDocument, title: String): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$title.docx")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/WordDocuments")
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    document.write(outputStream)
                }
                Toast.makeText(context, "Документ сохранен в памяти устройства${uri.path}", Toast.LENGTH_SHORT).show()
            }
            val file = File(uri?.path) // Uri path is not the file path, needs conversion
            return file
        } else {
            val file = createDocumentFile(title)
            saveDocumentToFile(document, file)
            return file
        }
    }
}