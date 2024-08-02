package com.betelgeuse.corp.examination.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.betelgeuse.corp.examination.R
import com.betelgeuse.corp.examination.add_work.TakerWork
import com.betelgeuse.corp.examination.dao.PhotoEntity
import com.bumptech.glide.Glide

class PhotoAdapter(private val onDeleteClick: TakerWork) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    interface OnItemClickListener {
        fun onDeleteItemClick(photo: PhotoEntity)
    }

    private val items = mutableListOf<PhotoEntity>()

    fun setItems(photos: List<PhotoEntity>) {
        Log.d("PhotoAdapter", "Setting items: ${photos.size}")
        items.clear()
        items.addAll(photos)
        notifyDataSetChanged()
    }

    fun getItems(): List<PhotoEntity> {
        return items
    }

    fun addItem(photo: PhotoEntity) {
        Log.d("PhotoAdapter", "Adding item: $photo")
        items.add(photo)
        notifyDataSetChanged()
    }

    fun removeItem(photo: PhotoEntity) {
        val position = items.indexOf(photo)
        if (position >= 0) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_photo_item, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imagePhoto)
        private val imageView2: ImageView = itemView.findViewById(R.id.imagePhoto2)
        private val commentView: TextView = itemView.findViewById(R.id.namePhoto)
        private val spView: TextView = itemView.findViewById(R.id.textView3)
        private val buildObjectID: TextView = itemView.findViewById(R.id.buildObjectID)
        private val typeRoom: TextView = itemView.findViewById(R.id.typeRoom)
        private val typeWork: TextView = itemView.findViewById(R.id.typeWork)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.itemDel)

        fun bind(photo: PhotoEntity) {
            commentView.text = photo.comment
            spView.text = photo.buildSpText
            buildObjectID.text = photo.buildObject
            typeRoom.text = photo.typeRoom
            typeWork.text = photo.typeWork

            photo.imageUri?.let {
                Glide.with(itemView.context)
                    .load(it)
                    .into(imageView)
            }

            photo.imageUri2?.let {
                Glide.with(itemView.context)
                    .load(it)
                    .into(imageView2)
            }

//            deleteButton.setOnClickListener {
//                onDeleteClick(photo)
//            }
        }
    }
}