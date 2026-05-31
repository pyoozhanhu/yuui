package com.vivo.album.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.vivo.album.R
import com.vivo.album.data.repository.AlbumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullscreenImageActivity : ComponentActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tvPageIndicator: TextView
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var btnFavorite: ImageView
    private lateinit var btnEdit: ImageView
    private var imagePaths: List<String> = emptyList()
    private var imageIds: List<String> = emptyList()
    private var startPosition: Int = 0
    private val repository by lazy { AlbumRepository(application) }
    private var currentFavoriteState: Boolean = false
    private var currentCategoryId: String? = null

    private val editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val newPath = result.data?.getStringExtra("edited_image_path")
            newPath?.let { updateCurrentImagePath(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        viewPager = findViewById(R.id.viewPager)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
        topBar = findViewById(R.id.topBar)
        bottomBar = findViewById(R.id.bottomBar)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnEdit = findViewById(R.id.btnEdit)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.btnShare).setOnClickListener {
            shareCurrentImage()
        }

        findViewById<ImageView>(R.id.btnDelete).setOnClickListener {
            deleteCurrentImage()
        }

        btnEdit.setOnClickListener {
            val currentPath = imagePaths[viewPager.currentItem]
            val intent = Intent(this, ImageEditActivity::class.java).apply {
                putExtra("image_path", currentPath)
            }
            editLauncher.launch(intent)
        }

        imagePaths = intent.getStringArrayListExtra("image_paths") ?: emptyList()
        startPosition = intent.getIntExtra("start_position", 0)
        val currentCategoryId = intent.getStringExtra("category_id")

        if (imagePaths.isEmpty()) {
            finish()
            return
        }

        viewPager.adapter = ImagePagerAdapter(imagePaths)
        viewPager.setCurrentItem(startPosition, false)
        updatePageIndicator(startPosition)
        
        updateFavoriteIcon(startPosition, currentCategoryId)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
                updateFavoriteIcon(position, currentCategoryId)
            }
        })

        viewPager.setOnClickListener {
            toggleBars()
        }

        btnFavorite.setOnClickListener {
            toggleFavorite(currentCategoryId)
        }
    }

    private fun getCurrentMediaId(position: Int): String? {
        val path = imagePaths.getOrNull(position) ?: return null
        return "image_${path.hashCode()}"
    }

    private fun updateFavoriteIcon(position: Int, categoryId: String?) {
        if (categoryId == null) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val mediaId = getCurrentMediaId(position)
            val isFavorite = mediaId?.let { repository.isFavorite(it, categoryId) } ?: false
            currentFavoriteState = isFavorite
            
            withContext(Dispatchers.Main) {
                btnFavorite.setImageResource(
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                )
            }
        }
    }

    private fun toggleFavorite(categoryId: String?) {
        if (categoryId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            val mediaId = getCurrentMediaId(viewPager.currentItem) ?: return@launch
            
            if (currentFavoriteState) {
                repository.removeFromFavorites(mediaId)
                currentFavoriteState = false
                withContext(Dispatchers.Main) {
                    btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                    Toast.makeText(this@FullscreenImageActivity, "已取消收藏", Toast.LENGTH_SHORT).show()
                }
            } else {
                repository.addToFavorites(mediaId)
                currentFavoriteState = true
                withContext(Dispatchers.Main) {
                    btnFavorite.setImageResource(R.drawable.ic_favorite)
                    Toast.makeText(this@FullscreenImageActivity, "已收藏", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareCurrentImage() {
        val path = imagePaths.getOrNull(viewPager.currentItem) ?: return
        
        // 支持 URI 和文件路径两种格式
        val uri = if (path.startsWith("content://")) {
            Uri.parse(path)
        } else {
            val file = File(path)
            if (!file.exists()) {
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "分享图片"))
    }

    private fun deleteCurrentImage() {
        val path = imagePaths.getOrNull(viewPager.currentItem) ?: return
        val mediaId = getCurrentMediaId(viewPager.currentItem)
        
        if (mediaId == null) {
            Toast.makeText(this, "无法获取图片 ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            repository.moveMediaToTrash(mediaId)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@FullscreenImageActivity, "已移动到最近删除", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateCurrentImagePath(newPath: String) {
        val currentPosition = viewPager.currentItem
        val mutablePaths = imagePaths.toMutableList()
        mutablePaths[currentPosition] = newPath
        imagePaths = mutablePaths
        
        (viewPager.adapter as? ImagePagerAdapter)?.notifyItemChanged(currentPosition)
        
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaId = getCurrentMediaId(currentPosition)
            if (mediaId != null) {
                repository.updateImagePath(mediaId, newPath)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@FullscreenImageActivity, "已保存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePageIndicator(position: Int) {
        tvPageIndicator.text = "${position + 1}/${imagePaths.size}"
    }

    private fun toggleBars() {
        val isVisible = topBar.visibility == View.VISIBLE
        val newVisibility = if (isVisible) View.GONE else View.VISIBLE
        topBar.visibility = newVisibility
        bottomBar.visibility = newVisibility
    }
}
