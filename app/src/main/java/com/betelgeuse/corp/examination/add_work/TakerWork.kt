package com.betelgeuse.corp.examination.add_work

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
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
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.util.Units
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
            saveDocumentToExternalStorage()
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

        checkAndRequestPermissions()
    }

    private fun saveDocumentToExternalStorage() {
        if (::adapter.isInitialized) {
            val doc = XWPFDocument()
            val dataToExport = collectDataForExport()
            val photos = adapter.getItems()
            addDataToDocument(doc, dataToExport, photos)

            // Сохранение документа в зависимости от версии API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveDocumentToExternalStorageQ(doc)
            } else {
                saveDocument(doc)
            }
        } else {
            // Обработка ситуации, когда adapter не инициализирован
        }
    }
    private fun saveDocumentToExternalStorageQ(doc: XWPFDocument) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "exported_data.docx")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/msword")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/WordDocuments")
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    doc.write(outputStream)
                }
                Toast.makeText(this, "Документ сохранен в ${uri.path}", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Ошибка при сохранении документа: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${applicationContext.packageName}")
                }
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
            }
        } else {
            // Запрос разрешений для чтения и записи внешнего хранилища
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            // Если есть разрешения, запрашиваем их у пользователя
            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            val permissionsDenied = mutableListOf<String>()
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    permissionsDenied.add(permissions[i])
                }
            }
            if (permissionsDenied.isNotEmpty()) {
                // Здесь можно добавить дополнительную логику в случае отказа в разрешении
                Toast.makeText(this, "Необходимо предоставить разрешения", Toast.LENGTH_SHORT).show()
            } else {
                // Разрешения получены, можно продолжать работу с файлами
                Toast.makeText(this, "Разрешения предоставлены", Toast.LENGTH_SHORT).show()
            }
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
            // Получение URI с помощью FileProvider
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
                Log.d("TakerWork", "Attempting to open InputStream for URI: $uri")
                val localUri = copyFileToInternalStorage(uri) ?: continue
                val inputStream = contentResolver.openInputStream(localUri)
                if (inputStream != null) {
                    Log.d("TakerWork", "Successfully opened InputStream for URI: $uri")
                    val pictureType = when (photoImageUri.substringAfterLast('.').lowercase()) {
                        "jpeg", "jpg" -> XWPFDocument.PICTURE_TYPE_JPEG
                        "png" -> XWPFDocument.PICTURE_TYPE_PNG
                        else -> XWPFDocument.PICTURE_TYPE_JPEG
                    }

                    val imageFileName = localUri.lastPathSegment
                    run.addPicture(inputStream, pictureType, imageFileName, Units.toEMU(200.0), Units.toEMU(200.0))
                    inputStream.close() // Не забывайте закрывать inputStream
                } else {
                    Log.d("TakerWork", "Failed to open InputStream for URI: $uri")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("TakerWork", "Exception: ${e.message}")
            }
        }
    }

    private fun saveDocument(doc: XWPFDocument) {
        val fileName = "exported_data.docx"
        val destinationDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WordDocuments")

        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val file = File(destinationDir, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                doc.write(outputStream)
            }
            Toast.makeText(this, "Документ сохранен в ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при сохранении документа: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val comment = data?.getStringExtra("comment") ?: ""
            val imageUri = data?.getStringExtra("imageUri") ?: ""

            CoroutineScope(Dispatchers.IO).launch {
                if (cardId == -1) {
                    val cardTitle = findViewById<EditText>(R.id.dataTextID).text.toString()
                    val newCard = CardEntity(title = cardTitle)
                    cardId = cardViewModel.insertSync(newCard)
                }

                // Copy image to internal storage and get new URI
                val newImageUri = copyFileToInternalStorage(Uri.parse(imageUri))
                if (newImageUri != null) {
                    val newItem = PhotoEntity(cardId = cardId, comment = comment, imageUri = newImageUri.toString())

                    Log.d("TakerWork", "Received comment: $comment")
                    Log.d("TakerWork", "Received imageUri: $newImageUri")

                    withContext(Dispatchers.Main) {
                        adapter.addItem(newItem)
                    }

                    photoDao.insert(newItem)
                } else {
                    Log.e("TakerWork", "Failed to copy image to internal storage")
                }
            }
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            val imageView: ImageView = findViewById(R.id.imagePhoto)
            imageView.setImageURI(selectedImageUri)
        }
    }

    companion object {
        const val ADD_FILE_REQUEST_CODE = 1
        const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 2
        const val READ_EXTERNAL_STORAGE_REQUEST_CODE = 3
        const val PICK_IMAGE_REQUEST = 4
        const val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 5
        const val REQUEST_PERMISSIONS_CODE = 6
    }
}