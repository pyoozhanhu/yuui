package com.vivo.album.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vivo.album.R
import com.vivo.album.data.Category

class VideoAdapter(
    private val onVideoClick: (categoryId: String, videos: List<String>, position: Int) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var categories: List<Category> = emptyList()

    fun submitList(list: List<Category>) {
        categories = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_category, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(categories[position], onVideoClick)
    }

    override fun getItemCount() = categories.size

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iv_cover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvCount: TextView = itemView.findViewById(R.id.tvCount)

        fun bind(category: Category, onClick: (categoryId: String, videos: List<String>, position: Int) -> Unit) {
            tvName.text = category.name
            tvCount.text = "${category.itemCount} 个视频"

            if (category.coverPath.isNotEmpty() && java.io.File(category.coverPath).exists()) {
                Glide.with(itemView.context)
                    .load(category.coverPath)
                    .into(iv_cover)
            } else {
                val firstVideo = category.items.firstOrNull()?.localPath
                if (firstVideo != null && java.io.File(firstVideo).exists()) {
                    Glide.with(itemView.context)
                        .load(firstVideo)
                        .into(iv_cover)
                } else {
                    iv_cover.setImageResource(R.drawable.ic_placeholder)
                }
            }

            itemView.setOnClickListener {
                val videos = category.items.map { it.localPath }
                onClick(category.id, videos, adapterPosition)
            }
        }
    }
}
