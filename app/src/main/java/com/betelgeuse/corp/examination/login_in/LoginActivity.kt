package com.betelgeuse.corp.examination.login_in

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.listwork.ListWork

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val loginButton: Button = findViewById(R.id.buttonLogin)
        loginButton.setOnClickListener {
            navigateToListWorkActivity()
        }
    }

    private fun navigateToListWorkActivity() {
        val intent = Intent(this, ListWork::class.java)
        startActivity(intent)
        finish()
    }
}