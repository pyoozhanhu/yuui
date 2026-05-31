package com.vivo.album.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vivo.album.R
import com.vivo.album.data.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ComicDetailActivity : ComponentActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var ivCover: ImageView
    private lateinit var ivFavorite: ImageView
    private lateinit var btnStartRead: Button
    private lateinit var btnAddTag: Button
    private lateinit var tagsContainer: LinearLayout
    private lateinit var rvPages: RecyclerView
    private var comicId: String = ""
    private var comicFolderPath: String = ""
    private var comicTitle: String = ""
    private var pagePaths: List<String> = emptyList()
    private var isFavorite: Boolean = false
    private val repository by lazy { AlbumRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comic_detail)

        tvTitle = findViewById(R.id.tvTitle)
        ivCover = findViewById(R.id.ivCover)
        ivFavorite = findViewById(R.id.ivFavorite)
        btnStartRead = findViewById(R.id.btnStartRead)
        btnAddTag = findViewById(R.id.btnAddTag)
        tagsContainer = findViewById(R.id.tagsContainer)
        rvPages = findViewById(R.id.rvPages)

        comicId = intent.getStringExtra("comic_id") ?: return
        comicTitle = intent.getStringExtra("comic_title") ?: "漫画"
        comicFolderPath = intent.getStringExtra("comic_folder") ?: return

        tvTitle.text = comicTitle

        loadCover()
        loadPages()
        loadFavoriteStatus()
        loadTags()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        ivFavorite.setOnClickListener {
            toggleFavorite()
        }

        btnStartRead.setOnClickListener {
            startReading()
        }

        btnAddTag.setOnClickListener {
            showAddTagDialog()
        }
    }

    private fun loadCover() {
        val firstImage = File(comicFolderPath).listFiles()
            ?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }
            ?.sortedBy { it.name }
            ?.firstOrNull()
        firstImage?.let {
            Glide.with(this).load(it).into(ivCover)
        }
    }

    private fun loadPages() {
        val imageFiles = File(comicFolderPath).listFiles()
            ?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }
            ?.sortedBy { it.name } ?: emptyList()
        pagePaths = imageFiles.map { it.absolutePath }
        rvPages.layoutManager = GridLayoutManager(this, 3)
        val adapter = PageThumbnailAdapter(pagePaths) { position ->
            startReadingFrom(position)
        }
        rvPages.adapter = adapter
    }

    private fun loadFavoriteStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            val fav = repository.isFavorite(comicId, getComicCategoryId())
            withContext(Dispatchers.Main) {
                isFavorite = fav
                updateFavoriteIcon()
            }
        }
    }

    private suspend fun getComicCategoryId(): String? {
        return repository.getAllCategories().find { it.categoryType == "comic" }?.id
    }

    private fun updateFavoriteIcon() {
        ivFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
    }

    private fun toggleFavorite() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isFavorite) {
                repository.removeFromFavorites(comicId)
            } else {
                repository.addToFavorites(comicId)
            }
            withContext(Dispatchers.Main) {
                isFavorite = !isFavorite
                updateFavoriteIcon()
                Toast.makeText(this@ComicDetailActivity, if (isFavorite) "已收藏" else "已取消收藏", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTags() {
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaItem = repository.getMediaItemById(comicId)
            val tags = mediaItem?.tags ?: emptyList()
            withContext(Dispatchers.Main) {
                displayTags(tags)
            }
        }
    }

    private fun displayTags(tags: List<String>) {
        tagsContainer.removeAllViews()
        for (tag in tags) {
            val chip = TextView(this).apply {
                text = tag
                setPadding(20, 8, 20, 8)
                setBackgroundResource(R.drawable.bg_tag)
                textSize = 12f
                setTextColor(getColor(R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8; bottomMargin = 8 }
            }
            tagsContainer.addView(chip)
        }
        if (tags.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "暂无标签，点击"添加标签""
                textSize = 12f
                setTextColor(getColor(R.color.text_muted))
            }
            tagsContainer.addView(emptyText)
        }
    }

    private fun showAddTagDialog() {
        val input = EditText(this)
        input.hint = "请输入标签"
        AlertDialog.Builder(this)
            .setTitle("添加标签")
            .setView(input)
            .setPositiveButton("添加") { _, _ ->
                val tag = input.text.toString().trim()
                if (tag.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val mediaItem = repository.getMediaItemById(comicId)
                        if (mediaItem != null) {
                            val newTags = mediaItem.tags.toMutableList().apply { add(tag) }
                            repository.updateMediaItem(mediaItem.copy(tags = newTags))
                            withContext(Dispatchers.Main) {
                                loadTags()
                                Toast.makeText(this@ComicDetailActivity, "标签已添加", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startReading() {
        startReadingFrom(0)
    }

    private fun startReadingFrom(position: Int) {
        val intent = Intent(this, ComicReadingActivity::class.java).apply {
            putStringArrayListExtra("image_paths", ArrayList(pagePaths))
            putExtra("start_position", position)
        }
        startActivity(intent)
    }

    inner class PageThumbnailAdapter(
        private val paths: List<String>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PageThumbnailAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_thumbnail, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(File(paths[position]))
                .centerCrop()
                .into(holder.imageView)
            holder.itemView.setOnClickListener {
                onItemClick(position)
            }
        }

        override fun getItemCount() = paths.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.ivThumb)
        }
    }
}
