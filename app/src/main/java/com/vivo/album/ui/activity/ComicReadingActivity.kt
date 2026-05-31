package com.vivo.album.ui.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.vivo.album.R
import java.io.File

class ComicReadingActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var topBar: View
    private lateinit var tvPageIndicator: TextView
    private var imagePaths: List<String> = emptyList()
    private var startPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comic_reading)

        viewPager = findViewById(R.id.viewPager)
        topBar = findViewById(R.id.topBar)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val folderPath = intent.getStringExtra("folder_path") ?: return
        startPosition = intent.getIntExtra("start_position", 0)

        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            finish()
            return
        }

        imagePaths = folder.listFiles { file ->
            file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
        }?.sortedBy { it.name }?.map { it.absolutePath } ?: emptyList()

        if (imagePaths.isEmpty()) {
            finish()
            return
        }

        viewPager.adapter = ComicPageAdapter()
        viewPager.setCurrentItem(startPosition, false)
        updatePageIndicator(startPosition)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
            }
        })

        viewPager.setOnClickListener {
            topBar.visibility = if (topBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun updatePageIndicator(position: Int) {
        tvPageIndicator.text = "${position + 1}/${imagePaths.size}"
    }

    inner class ComicPageAdapter : RecyclerView.Adapter<ComicPageAdapter.PageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val photoView = PhotoView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            return PageViewHolder(photoView)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val path = imagePaths[position]
            Glide.with(holder.photoView.context)
                .load(File(path))
                .into(holder.photoView)
        }

        override fun getItemCount() = imagePaths.size

        inner class PageViewHolder(val photoView: PhotoView) : RecyclerView.ViewHolder(photoView)
    }
}
