package com.betelgeuse.corp.examination.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.dao.PhotoEntity
import com.bumptech.glide.Glide

class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private val items = mutableListOf<PhotoEntity>()

    fun setItems(photos: List<PhotoEntity>) {
        items.clear()
        items.addAll(photos)
        notifyDataSetChanged()
    }

    fun getItems(): List<PhotoEntity> {
        return items
    }

    fun addItem(photo: PhotoEntity) {
        items.add(photo)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_photo_item, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imagePhoto)
        private val commentView: TextView = itemView.findViewById(R.id.namePhoto)

        fun bind(photo: PhotoEntity) {
            commentView.text = photo.comment
            photo.imageUri?.let {
                Log.d("PhotoAdapter", "Loading image from URI: $it")
                Glide.with(itemView.context)
                    .load(it)
                    .into(imageView)
            }
        }
    }
}
