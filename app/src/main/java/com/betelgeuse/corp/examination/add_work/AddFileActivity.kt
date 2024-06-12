package com.betelgeuse.corp.examination.add_work

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.dao.PhotoDao
import com.betelgeuse.corp.examination.dao.PhotoEntity

class AddFileActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private val PICK_IMAGE_REQUEST = 1
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

        val addImageButton: Button = findViewById(R.id.addImageID)
        imageView = findViewById(R.id.imageViewID)

        addImageButton.setOnClickListener {
            openGallery()
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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            imageView.setImageURI(selectedImageUri)
        }
    }
}