package com.vivo.album.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vivo.album.R
import com.vivo.album.data.Category
import com.vivo.album.data.MediaItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class CategoryAdapter(
    private val onHeaderClick: (Category, Int) -> Unit,
    private val onThumbClick: (MediaItem, Int) -> Unit,
    private val onCategorySelect: (Category, Boolean) -> Unit,
    private val onItemSelect: (MediaItem, String, Boolean) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    var categories: List<Category> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isSelectMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position)
    }

    override fun getItemCount() = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.ivCover)
        private val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvCount: TextView = itemView.findViewById(R.id.tvItemCount)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        private val cbSelectCategory: CheckBox = itemView.findViewById(R.id.cbSelectCategory)
        private val rvThumbsCollapsed: RecyclerView = itemView.findViewById(R.id.rvThumbsCollapsed)
        private val rvThumbsExpanded: RecyclerView = itemView.findViewById(R.id.rvThumbsExpanded)
        private var collapsedAdapter: ThumbnailAdapter? = null
        private var expandedAdapter: ThumbnailAdapter? = null

        fun bind(category: Category, position: Int) {
            tvName.text = category.name
            tvCount.text = "${category.items.size} 张照片"

            Glide.with(itemView.context)
                .load(category.coverPath)
                .placeholder(R.drawable.ic_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivCover)

            cbSelectCategory.visibility = if (isSelectMode) View.VISIBLE else View.GONE
            cbSelectCategory.isChecked = selectedIds.contains("category_${category.id}")
            cbSelectCategory.setOnCheckedChangeListener { _, isChecked ->
                onCategorySelect(category, isChecked)
            }

            val collapsedList = category.items.take(4)
            if (collapsedAdapter == null) {
                val horizontalLayout = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
                rvThumbsCollapsed.layoutManager = horizontalLayout
                collapsedAdapter = ThumbnailAdapter(
                    layoutType = ThumbnailAdapter.LayoutType.HORIZONTAL,
                    isSelectMode = { isSelectMode },
                    selectedIds = { selectedIds },
                    categoryId = category.id,
                    onItemClick = { mediaItem -> onThumbClick(mediaItem, position) },
                    onItemSelect = { mediaItem, isChecked -> onItemSelect(mediaItem, category.id, isChecked) }
                )
                rvThumbsCollapsed.adapter = collapsedAdapter
            }
            collapsedAdapter?.submitList(collapsedList)

            if (expandedAdapter == null) {
                val gridLayout = GridLayoutManager(itemView.context, 3)
                rvThumbsExpanded.layoutManager = gridLayout
                expandedAdapter = ThumbnailAdapter(
                    layoutType = ThumbnailAdapter.LayoutType.GRID,
                    isSelectMode = { isSelectMode },
                    selectedIds = { selectedIds },
                    categoryId = category.id,
                    onItemClick = { mediaItem -> onThumbClick(mediaItem, position) },
                    onItemSelect = { mediaItem, isChecked -> onItemSelect(mediaItem, category.id, isChecked) }
                )
                rvThumbsExpanded.adapter = expandedAdapter
            }
            expandedAdapter?.submitList(category.items)

            if (category.isExpanded) {
                rvThumbsCollapsed.visibility = View.GONE
                rvThumbsExpanded.visibility = View.VISIBLE
                ivExpand.rotation = 180f
            } else {
                rvThumbsCollapsed.visibility = View.VISIBLE
                rvThumbsExpanded.visibility = View.GONE
                ivExpand.rotation = 0f
            }

            itemView.findViewById<View>(R.id.categoryHeader).setOnClickListener {
                if (isSelectMode) {
                    cbSelectCategory.isChecked = !cbSelectCategory.isChecked
                } else {
                    onHeaderClick(category, position)
                }
            }
        }
    }
}
