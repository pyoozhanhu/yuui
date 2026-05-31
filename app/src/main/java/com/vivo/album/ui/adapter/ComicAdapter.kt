package com.vivo.album.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.widget.CheckBox
import androidx.core.content.ContextCompat
import com.vivo.album.R
import com.vivo.album.data.Category
import com.vivo.album.data.MediaItem
import java.io.File

class ComicAdapter(
    private val onComicClick: (MediaItem) -> Unit,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<ComicAdapter.ComicViewHolder>() {

    private var categories: List<Category> = emptyList()
    private var isSelectMode = false
    private var selectedIds = setOf<String>()

    fun setSelectMode(selectMode: Boolean, selected: Set<String>) {
        isSelectMode = selectMode
        selectedIds = selected
        notifyDataSetChanged()
    }

    fun submitList(list: List<Category>) {
        categories = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comic_category, parent, false)
        return ComicViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComicViewHolder, position: Int) {
        val category = categories[position]
        val firstMediaItem = category.items.firstOrNull()
        if (firstMediaItem != null) {
            holder.bind(firstMediaItem, category, onComicClick)
        }
    }

    override fun getItemCount() = categories.size

    inner class ComicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)

        fun bind(mediaItem: MediaItem, category: Category, onClick: (MediaItem) -> Unit) {
            tvName.text = category.name

            if (category.coverPath.isNotEmpty() && File(category.coverPath).exists()) {
                Glide.with(itemView.context)
                    .load(File(category.coverPath))
                    .into(ivCover)
            } else if (mediaItem.localPath.isNotEmpty() && File(mediaItem.localPath).exists()) {
                val folder = File(mediaItem.localPath)
                val firstImage = folder.listFiles()
                    ?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }
                    ?.sortedBy { it.name }
                    ?.firstOrNull()
                if (firstImage != null) {
                    Glide.with(itemView.context)
                        .load(firstImage)
                        .into(ivCover)
                } else {
                    ivCover.setImageResource(R.drawable.ic_photo)
                }
            } else {
                ivCover.setImageResource(R.drawable.ic_photo)
            }

            if (isSelectMode) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.isChecked = selectedIds.contains(mediaItem.id)
                cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    onSelectionChanged(mediaItem.id, isChecked)
                }
                itemView.setOnClickListener {
                }
            } else {
                cbSelect.visibility = View.GONE
                cbSelect.isChecked = false
                itemView.setOnClickListener {
                    onClick(mediaItem)
                }
            }
        }
    }
}
