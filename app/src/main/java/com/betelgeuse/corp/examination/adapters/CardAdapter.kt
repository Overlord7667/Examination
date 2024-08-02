package com.betelgeuse.corp.examination.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.dao.CardEntity

class CardAdapter(
    private var cards: MutableList<CardEntity>,
    private val onEditClick: (CardEntity) -> Unit,
    private val onDeleteClick: (CardEntity) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    fun setCards(newCards: List<CardEntity>) {
        cards.clear()
        cards.addAll(newCards)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_file_item, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int = cards.size

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameWorkItem)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteBTN)

        fun bind(card: CardEntity) {
            nameTextView.text = card.title // Отображение имени карточки
            itemView.setOnClickListener { onEditClick(card) }
            deleteButton.setOnClickListener { onDeleteClick(card) }
        }
    }
}
