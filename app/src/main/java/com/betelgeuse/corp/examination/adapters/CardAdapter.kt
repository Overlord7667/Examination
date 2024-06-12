package com.betelgeuse.corp.examination.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.dao.CardEntity

class CardAdapter(private var cardList: List<CardEntity>, private val onItemClick: (CardEntity) -> Unit) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.nameWorkItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_file_item, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val currentCard = cardList[position]
        holder.titleTextView.text = currentCard.title
        Log.d("CardAdapter", "Binding card title: ${currentCard.title}")

        holder.itemView.setOnClickListener {
            onItemClick(currentCard)
        }
    }

    override fun getItemCount(): Int {
        return cardList.size
    }

    fun setCards(cards: List<CardEntity>) {
        this.cardList = cards
        notifyDataSetChanged()
        Log.d("CardAdapter", "setCards called with ${cards.size} cards")
    }
}
