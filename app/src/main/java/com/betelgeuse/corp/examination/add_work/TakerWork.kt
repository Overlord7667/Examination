package com.betelgeuse.corp.examination.add_work

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.adapters.PhotoAdapter
import com.betelgeuse.corp.examination.dao.CardDatabase
import com.betelgeuse.corp.examination.dao.CardEntity
import com.betelgeuse.corp.examination.dao.PhotoDao
import com.betelgeuse.corp.examination.dao.PhotoEntity
import com.betelgeuse.corp.examination.models.CardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class TakerWork : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private lateinit var photoDao: PhotoDao
    private lateinit var cardViewModel: CardViewModel
    private var cardId: Int = -1
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_taker_work)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cardViewModel = ViewModelProvider(this).get(CardViewModel::class.java)
        photoDao = CardDatabase.getDatabase(this).photoDao()

        recyclerView = findViewById(R.id.photoID)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PhotoAdapter()
        recyclerView.adapter = adapter

        cardId = intent.getIntExtra("card_id", -1)

        if (cardId == -1) {
            adapter.setItems(emptyList())
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val photos = photoDao.getPhotosByCardIdSync(cardId)
                withContext(Dispatchers.Main) {
                    adapter.setItems(photos)
                }
            }
        }

        val addButtonFile: ImageButton = findViewById(R.id.addFile)
        addButtonFile.setOnClickListener {
            val intent = Intent(this, AddFileActivity::class.java)
            startActivityForResult(intent, ADD_FILE_REQUEST_CODE)
        }

        val saveButton: Button = findViewById(R.id.saveBTN)
        val dataText: EditText = findViewById(R.id.dataTextID)
        val cardTitle = intent.getStringExtra("card_title")
        if (!cardTitle.isNullOrEmpty()) {
            dataText.setText(cardTitle)
        }

        val exportToWordButton: ImageButton = findViewById(R.id.tables)
        exportToWordButton.setOnClickListener {
            exportDataToWord()
            Log.d("TakerWork", "Кнопка работает")
        }

        saveButton.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("card_id", cardId)
            resultIntent.putExtra("title", dataText.text.toString())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST_CODE, WRITE_EXTERNAL_STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TakerWork", "Разрешение было предоставлено")
                } else {
                    Toast.makeText(this, "Разрешение не было предоставлено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun exportDataToWord() {
        if (::adapter.isInitialized) {
            val doc = XWPFDocument()
            val dataToExport = collectDataForExport()
            val photos = adapter.getItems()
            addDataToDocument(doc, dataToExport, photos)
            saveDocument(doc)
        } else {
            // Обработка ситуации, когда adapter не инициализирован
        }
    }

    private fun collectDataForExport(): List<String> {
        val dataToExport = mutableListOf<String>()
        val photos = adapter.getItems()

        for (photo in photos) {
            val comment = photo.comment ?: ""
            dataToExport.add(comment)
        }

        return dataToExport
    }

    private fun copyFileToInternalStorage(uri: Uri): Uri? {
        val destinationPath = File(filesDir, "images")
        if (!destinationPath.exists()) {
            destinationPath.mkdirs()
        }

        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(destinationPath, fileName)

        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            FileProvider.getUriForFile(this, "${packageName}.provider", destinationFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun addDataToDocument(doc: XWPFDocument, dataToExport: List<String>, photos: List<PhotoEntity>) {
        for (i in photos.indices) {
            val photo = photos[i]
            val comment = dataToExport[i]

            val paragraph = doc.createParagraph()
            val run = paragraph.createRun()
            run.setText(comment)
            run.addBreak()

            val photoImageUri = photo.imageUri ?: continue
            try {
                val uri = Uri.parse(photoImageUri)
                val localUri = copyFileToInternalStorage(uri) ?: continue
                val inputStream = contentResolver.openInputStream(localUri)
                if (inputStream != null) {
                    val pictureType = when (photoImageUri.substringAfterLast('.').lowercase()) {
                        "jpeg", "jpg" -> XWPFDocument.PICTURE_TYPE_JPEG
                        "png" -> XWPFDocument.PICTURE_TYPE_PNG
                        else -> XWPFDocument.PICTURE_TYPE_JPEG
                    }

                    val imageFileName = localUri.lastPathSegment
                    run.addPicture(inputStream, pictureType, imageFileName, Units.toEMU(200.0), Units.toEMU(200.0))
                    inputStream.close() // Don't forget to close the inputStream
                } else {
                    Log.d("TakerWork", "Не удалось открыть InputStream для $uri")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("TakerWork", "Exception: ${e.message}")
            }
        }
    }

    private fun saveDocument(doc: XWPFDocument) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
            return
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "exported_data.docx")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val contentResolver = contentResolver
        val uri = contentResolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), contentValues)

        uri?.let {
            try {
                contentResolver.openOutputStream(it).use { outputStream ->
                    doc.write(outputStream)
                    Toast.makeText(this, "Данные успешно выгружены в Word", Toast.LENGTH_SHORT).show()
                    Log.d("TakerWork", "saveDocument: ${uri.toString()}")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("TakerWork", "Not saveDocument: ${uri.toString()}")
                Toast.makeText(this, "Ошибка при выгрузке данных в Word: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Ошибка при создании файла", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            val comment = data?.getStringExtra("comment") ?: ""
            val imageUri = data?.getStringExtra("imageUri") ?: ""

            CoroutineScope(Dispatchers.IO).launch {
                if (cardId == -1) {
                    val cardTitle = findViewById<EditText>(R.id.dataTextID).text.toString()
                    val newCard = CardEntity(title = cardTitle)
                    cardId = cardViewModel.insertSync(newCard)
                }

                val newItem = PhotoEntity(cardId = cardId, comment = comment, imageUri = imageUri)

                Log.d("TakerWork", "Received comment: $comment")
                Log.d("TakerWork", "Received imageUri: $imageUri")

                if (comment.isNotEmpty() || imageUri.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        adapter.addItem(newItem)
                    }
                }

                photoDao.insert(newItem)
            }
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            // Предположительно, imageView - это ImageView для отображения выбранного изображения
            val imageView: ImageView = findViewById(R.id.imagePhoto)
            imageView.setImageURI(selectedImageUri)
        }
    }

    companion object {
        const val ADD_FILE_REQUEST_CODE = 1
        const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 2
        const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 3
        const val PICK_IMAGE_REQUEST = 4
    }
}