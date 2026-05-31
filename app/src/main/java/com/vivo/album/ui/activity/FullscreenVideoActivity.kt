package com.vivo.album.ui.activity

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.vivo.album.R
import java.io.File
import kotlin.math.abs

class FullscreenVideoActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var topBar: View
    private lateinit var tvPageIndicator: TextView
    private var videoPaths: List<String> = emptyList()
    private var startPosition: Int = 0
    private val playerRefs = mutableMapOf<Int, ExoPlayer?>()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_video)

        prefs = getSharedPreferences("video_progress", MODE_PRIVATE)

        viewPager = findViewById(R.id.viewPager)
        topBar = findViewById(R.id.topBar)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        videoPaths = intent.getStringArrayListExtra("video_paths") ?: emptyList()
        startPosition = intent.getIntExtra("start_position", 0)

        if (videoPaths.isEmpty()) {
            finish()
            return
        }

        viewPager.adapter = VideoPagerAdapter()
        viewPager.setCurrentItem(startPosition, false)
        updatePageIndicator(startPosition)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position)
                releaseFarPlayers(position)
            }
        })

        viewPager.setOnClickListener {
            topBar.visibility = if (topBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun updatePageIndicator(position: Int) {
        tvPageIndicator.text = "${position + 1}/${videoPaths.size}"
    }

    private fun releaseFarPlayers(currentPos: Int) {
        playerRefs.keys.forEach { pos ->
            if (abs(pos - currentPos) > 1) {
                playerRefs[pos]?.release()
                playerRefs[pos] = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerRefs.values.forEach { it?.release() }
        playerRefs.clear()
    }

    inner class VideoPagerAdapter : RecyclerView.Adapter<VideoPagerAdapter.VideoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val playerView = PlayerView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = true
                resizeMode = PlayerView.RESIZE_MODE_FIT
            }
            return VideoViewHolder(playerView)
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val path = videoPaths[position]
            holder.bind(path, position)
        }

        override fun getItemCount() = videoPaths.size

        inner class VideoViewHolder(private val playerView: PlayerView) :
            RecyclerView.ViewHolder(playerView) {
            private var player: ExoPlayer? = null
            private var currentMediaId: String? = null

            fun bind(path: String, position: Int) {
                val existing = playerRefs[position]
                if (existing != null) {
                    player = existing
                    playerView.player = player
                    return
                }

                player = ExoPlayer.Builder(playerView.context).build().also {
                    playerView.player = it
                    playerRefs[position] = it
                }

                val file = File(path)
                if (!file.exists()) {
                    return
                }

                currentMediaId = "video_$position"
                val uri = Uri.fromFile(file)
                val mediaItem = MediaItem.fromUri(uri)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                
                val savedPos = getPlaybackPosition(currentMediaId!!)
                if (savedPos > 0) {
                    player?.seekTo(savedPos)
                }
                
                player?.playWhenReady = (position == viewPager.currentItem)
            }
        }
    }

    private fun savePlaybackPosition(mediaId: String, position: Long) {
        prefs.edit().putLong(mediaId, position).apply()
    }

    private fun getPlaybackPosition(mediaId: String): Long {
        return prefs.getLong(mediaId, 0)
    }

    override fun onPause() {
        super.onPause()
        playerRefs.forEach { (pos, player) ->
            if (player != null) {
                val mediaId = "video_$pos"
                savePlaybackPosition(mediaId, player.currentPosition)
            }
        }
    }
}
