package com.betelgeuse.corp.examination.listwork

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.adapters.CardAdapter
import com.betelgeuse.corp.examination.add_work.TakerWork
import com.betelgeuse.corp.examination.dao.CardEntity
import com.betelgeuse.corp.examination.models.CardViewModel

class ListWork : AppCompatActivity() {

    private lateinit var cardViewModel: CardViewModel
    private lateinit var adapter: CardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_work)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val addButton: Button = findViewById(R.id.addID)
        addButton.setOnClickListener {
            val intent = Intent(this, TakerWork::class.java)
            startActivityForResult(intent, REQUEST_CODE_ADD)
        }

        adapter = CardAdapter(mutableListOf(),
            { selectedCard ->
                val intent = Intent(this, TakerWork::class.java)
                intent.putExtra("card_id", selectedCard.id)
                intent.putExtra("card_title", selectedCard.title)
                startActivityForResult(intent, REQUEST_CODE_EDIT)
            },
            { selectedCard ->
                cardViewModel.delete(selectedCard) // Удаление карточки
            })

        val recyclerView: RecyclerView = findViewById(R.id.workID)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        cardViewModel = ViewModelProvider(this).get(CardViewModel::class.java)
        cardViewModel.allCards.observe(this, Observer { cards ->
            cards?.let {
                adapter.setCards(it)
                Log.d("ListWork", "Received cards: ${cards.size}")
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD && resultCode == Activity.RESULT_OK) {
            cardViewModel.loadCards() // Обновляем список карточек
        } else if (requestCode == REQUEST_CODE_EDIT && resultCode == Activity.RESULT_OK) {
            val cardId = data?.getIntExtra("card_id", -1) ?: -1
            val cardTitle = data?.getStringExtra("title")

            if (cardId != -1 && cardTitle != null) {
                val updatedCard = CardEntity(id = cardId, title = cardTitle)
                cardViewModel.update(updatedCard)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_ADD = 1
        private const val REQUEST_CODE_EDIT = 2
    }
}