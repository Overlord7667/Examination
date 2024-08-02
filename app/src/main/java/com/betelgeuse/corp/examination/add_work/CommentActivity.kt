package com.betelgeuse.corp.examination.add_work

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.betelgeuse.corp.examination.R

class CommentActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var commentEditText: EditText
    private var imageUri: Uri? = null
    private var initialComment: String? = null
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_comment)

        if (hasNavigationBar()) {
            val saveButton = findViewById<Button>(R.id.saveCommentButton)
            val params = saveButton.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = resources.getDimensionPixelSize(R.dimen.system_ui_bottom_padding)
            saveButton.layoutParams = params
        }

        imageView = findViewById(R.id.imageViewComment)
        commentEditText = findViewById(R.id.commentEditText)

        val imageUriString = intent.getStringExtra("imageUri")
        initialComment = intent.getStringExtra("comment")
        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString)
            Log.d("CommentActivity", "Image URI: $imageUri")
            imageView.setImageURI(imageUri)
        } else {
            Log.e("CommentActivity", "Image URI is null")
            Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
        }

        val saveButton: Button = findViewById(R.id.saveCommentButton)
        saveButton.setOnClickListener {
            val comment = commentEditText.text.toString()
            val resultIntent = Intent().apply {
                if (comment.isNotEmpty()) {
                    putExtra("comment", comment)
                } else {
                    putExtra("comment", initialComment)
                }
                putExtra("imageUri", imageUri.toString())
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        val cancelButton: Button = findViewById(R.id.cancelCommentButton)
        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun hasNavigationBar(): Boolean {
        val display = windowManager.defaultDisplay
        val realDisplayMetrics = DisplayMetrics()
        display.getRealMetrics(realDisplayMetrics)
        val realHeight = realDisplayMetrics.heightPixels
        val realWidth = realDisplayMetrics.widthPixels
        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels
        return realWidth - displayWidth > 0 || realHeight - displayHeight > 0
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }
}