package com.betelgeuse.corp.examination.add_work

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.betelgeuse.corp.examination.R

class SP_Build : AppCompatActivity() {
    private val checkBoxList = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sp_build)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Получение данных из Intent
        val dataList = intent.getStringArrayListExtra("dataList") ?: emptyList<String>()
        setupCards(dataList)

        val buttonAddSP: Button = findViewById(R.id.buttonAddSP)
        buttonAddSP.setOnClickListener {
            val selectedItems = checkBoxList.filter { it.isChecked }.map { checkBox ->
                val cardView = checkBox.parent as View
                val textView: TextView = cardView.findViewById(R.id.textView4)
                textView.text.toString()
            }

            val resultIntent = Intent().apply {
                putStringArrayListExtra("selectedItems", ArrayList(selectedItems))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        // Добавляем обработчик для кнопки "Отмена"
        val cancelButton: Button = findViewById(R.id.cancelSP)
        cancelButton.setOnClickListener {
            // Закрываем активность без передачи данных
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun setupCards(dataList: List<String>) {
        val container = findViewById<LinearLayout>(R.id.linearLayoutContainer)

        dataList.forEach { data ->
            val cardView = layoutInflater.inflate(R.layout.card_sp_items, container, false) as CardView
            val textView: TextView = cardView.findViewById(R.id.textView4)
            val checkBox: CheckBox = cardView.findViewById(R.id.checkBox5)
            checkBoxList.add(checkBox)
            textView.text = data

            // Обработка нажатий на CheckBox
            checkBox.setOnClickListener {
                // Состояние checkBox.isChecked автоматически изменяется системой
            }

            container.addView(cardView)
        }
    }
}