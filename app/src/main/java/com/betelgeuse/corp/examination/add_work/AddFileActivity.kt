package com.betelgeuse.corp.examination.add_work

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.dao.PhotoDao
import java.io.File
import java.io.FileOutputStream

class AddFileActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_IMAGE_CAPTURE = 2
    private var selectedImageUri: Uri? = null
    private lateinit var photoDao: PhotoDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_file)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageView = findViewById(R.id.imageViewID)

        val addImageButton: Button = findViewById(R.id.addImageID)
        addImageButton.setOnClickListener {
            showImageSourceOptions()
        }

        val cancelButton: Button = findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener {
            finish()  // Завершает текущую активность и возвращает пользователя к предыдущей
        }

        val addFileButton: Button = findViewById(R.id.addFileInFile)
        addFileButton.setOnClickListener {
            val comment = findViewById<EditText>(R.id.commentID).text.toString()
            val imageUriString = selectedImageUri?.toString()
            val resultIntent = Intent().apply {
                putExtra("comment", comment)
                putExtra("imageUri", imageUriString)
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun showImageSourceOptions() {
        val options = arrayOf("Камера", "Галерея")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите источник")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
            }
        }
        builder.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedImageUri = data?.data
                    imageView.setImageURI(selectedImageUri)
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    selectedImageUri = saveImageToInternalStorage(imageBitmap)
                    imageView.setImageURI(selectedImageUri)
                }
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val destinationPath = File(filesDir, "images")
        if (!destinationPath.exists()) {
            destinationPath.mkdirs()
        }
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(destinationPath, fileName)
        FileOutputStream(destinationFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        return FileProvider.getUriForFile(this, "${packageName}.provider", destinationFile)
    }
}