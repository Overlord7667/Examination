package com.betelgeuse.corp.examination.add_work

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.betelgeuse.corp.examination.documents_build.DocumentHelper
import com.betelgeuse.corp.examination.documents_build.DocumentManager
import com.betelgeuse.corp.examination.models.CardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TakerWork : AppCompatActivity(), PhotoAdapter.OnDeleteClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private lateinit var photoDao: PhotoDao
    private lateinit var cardViewModel: CardViewModel
    private var cardId: Int = -1
    private var selectedImageUri: Uri? = null
    private lateinit var dataText: EditText
    private lateinit var documentManager: DocumentManager
    private lateinit var documentHelper: DocumentHelper
    private var permissionsGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_taker_work)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d("TakerWork", "onCreate called")

        dataText = findViewById(R.id.dataTextID)
        cardViewModel = ViewModelProvider(this).get(CardViewModel::class.java)
        photoDao = CardDatabase.getDatabase(this).photoDao()
        recyclerView = findViewById(R.id.photoID)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PhotoAdapter(this)
        recyclerView.adapter = adapter

        documentManager = DocumentManager(this)
        documentHelper = DocumentHelper(this, adapter)

        cardId = intent.getIntExtra("card_id", -1)
        val cardTitle = intent.getStringExtra("card_title")
        if (cardTitle != null) {
            dataText.setText(cardTitle) // Устанавливаем имя карточки в EditText
        }

        initializeButtons()

        checkAndRequestPermissions()
    }

    private fun initializeButtons() {
        val addButtonFile: ImageButton = findViewById(R.id.addFile)
        val saveButton: Button = findViewById(R.id.saveBTN)
        val sendButton: Button = findViewById(R.id.sendBTN)
        val exportToWordButton: ImageButton = findViewById(R.id.tables)

        addButtonFile.setOnClickListener {
            val intent = Intent(this, AddFileActivity::class.java)
            startActivityForResult(intent, ADD_FILE_REQUEST_CODE)
        }

        saveButton.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("card_id", cardId)
                putExtra("title", dataText.text.toString())
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        sendButton.setOnClickListener {
            sendDocument()
        }

        exportToWordButton.setOnClickListener {
            saveDocumentToExternalStorage()
        }
    }

    private fun saveDocumentToExternalStorage() {
        val document = XWPFDocument()
        val dataToExport = documentHelper.collectDataForExport()
        val photos = adapter.getItems()

        documentHelper.addDataToDocument(document, dataToExport, photos)

        val title = dataText.text.toString()
        documentManager.saveDocumentToExternalStorage(document, title)
    }

    private fun sendDocument() {
        val document = XWPFDocument()
        val dataToExport = documentHelper.collectDataForExport()
        val photos = adapter.getItems()

        documentHelper.addDataToDocument(document, dataToExport, photos)

        val title = dataText.text.toString().trim()
        val fileName = if (title.isNotEmpty()) "$title.docx" else "document.docx"

        try {
            val cacheFile = File(cacheDir, fileName).apply {
                if (!exists()) {
                    createNewFile()
                }
            }

            FileOutputStream(cacheFile).use { outputStream ->
                document.write(outputStream)
            }

            documentManager.shareFile(cacheFile)

        } catch (e: IOException) {
            Log.e("TakerWork", "Ошибка при создании документа: ${e.message}")
            Toast.makeText(this, "Ошибка при отправке документа: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        } else {
            permissionsGranted = true
            proceedWithAppLogic()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${applicationContext.packageName}")
                }
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
            } else {
                permissionsGranted = true
                proceedWithAppLogic()
            }
        }
    }

    private fun proceedWithAppLogic() {
        Log.d("TakerWork", "Proceeding with app logic, cardId: $cardId")
        if (cardId == -1) {
            adapter.setItems(emptyList())
        } else {
            photoDao.getPhotosByCardId(cardId).observe(this, { photos ->
                Log.d("TakerWork", "Photos loaded: ${photos?.size}")
                adapter.setItems(photos ?: emptyList())
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            permissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (permissionsGranted) {
                proceedWithAppLogic()
            } else {
//                Toast.makeText(this, "Необходимо предоставить разрешения", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDeleteItemClick(photo: PhotoEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            val rowsDeleted = cardViewModel.deletePhoto(photo)
            withContext(Dispatchers.Main) {
                if (rowsDeleted > 0) {
                    adapter.removeItem(photo)
                }
            }
        }
    }
    override fun showDeleteConfirmationDialog(photo: PhotoEntity) {
        AlertDialog.Builder(this)
            .setTitle("Удалить")
            .setMessage("Вы уверены, что хотите удалить этот элемент?")
            .setPositiveButton("Удалить") { _, _ ->
                // Если пользователь подтвердил удаление, вызываем onDeleteItemClick
                onDeleteItemClick(photo)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val comment = data?.getStringExtra("comment") ?: ""
            val imageUri = data?.getStringExtra("imageUri") ?: ""
            val imageUri2 = data?.getStringExtra("imageUri2") ?: ""
            val buildSpText = data?.getStringExtra("buildSpText") ?: ""
            val buildRoom = data?.getStringExtra("selectedBuildRoom") ?: ""
            val buildObject = data?.getStringExtra("selectedTypeObject") ?: ""
            val typeRoom = data?.getStringExtra("selectedTypeRoom") ?: ""
            val typeWork = data?.getStringExtra("selectedTypeWork") ?: ""

            CoroutineScope(Dispatchers.IO).launch {
                if (cardId == -1) {
                    val cardTitle = dataText.text.toString()
                    cardId = cardViewModel.insertSync(CardEntity(title = cardTitle))
                }

                val newImageUri = documentHelper.copyFileToInternalStorage(Uri.parse(imageUri))
                val newImageUri2 = documentHelper.copyFileToInternalStorage(Uri.parse(imageUri2))
                if (newImageUri != null && newImageUri2 != null) {
                    val newItem = PhotoEntity(
                        cardId = cardId,
                        comment = comment,
                        imageUri = newImageUri.toString(),
                        imageUri2 = newImageUri2.toString(),
                        buildSpText = buildSpText,
                        buildRoom = buildRoom,
                        buildObject = buildObject,
                        typeRoom = typeRoom,
                        typeWork = typeWork
                    )

                    withContext(Dispatchers.Main) {
                        adapter.addItem(newItem)
                    }
                    photoDao.insert(newItem)
                }
            }
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            findViewById<ImageView>(R.id.imagePhoto).setImageURI(selectedImageUri)
        }
    }

    companion object {
        const val ADD_FILE_REQUEST_CODE = 1
        const val REQUEST_PERMISSIONS_CODE = 2
        const val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 3
        const val PICK_IMAGE_REQUEST = 4
    }
}