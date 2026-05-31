package com.vivo.album.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.vivo.album.R
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageEditActivity : ComponentActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var btnCancel: ImageView
    private lateinit var btnSave: TextView
    private lateinit var btnCrop: Button
    private lateinit var btnFilter: Button
    private lateinit var btnAdjust: Button
    private lateinit var btnRotate: Button
    
    private var originalPath: String = ""
    private var currentBitmap: Bitmap? = null
    private var currentFilterIndex: Int = -1
    private val filterMatrices = listOf(
        floatArrayOf( // 原图
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        floatArrayOf( // 黑白
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        floatArrayOf( // 深褐色
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ),
        floatArrayOf( // 反色
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ),
        floatArrayOf( // 怀旧
            0.272f, 0.534f, 0.131f, 0f, 30f,
            0.272f, 0.534f, 0.131f, 0f, 20f,
            0.272f, 0.534f, 0.131f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    private val filterNames = listOf("原图", "黑白", "深褐色", "反色", "怀旧")

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                currentBitmap = BitmapFactory.decodeFile(it.path)
                photoView.setImageBitmap(currentBitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_edit)

        photoView = findViewById(R.id.photoView)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)
        btnCrop = findViewById(R.id.btnCrop)
        btnFilter = findViewById(R.id.btnFilter)
        btnAdjust = findViewById(R.id.btnAdjust)
        btnRotate = findViewById(R.id.btnRotate)

        originalPath = intent.getStringExtra("image_path") ?: return

        loadImage()

        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveImage() }
        btnCrop.setOnClickListener { startCrop() }
        btnFilter.setOnClickListener { showFilterDialog() }
        btnAdjust.setOnClickListener { showAdjustDialog() }
        btnRotate.setOnClickListener { rotateImage() }
    }

    private fun loadImage() {
        lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = if (originalPath.startsWith("content://")) {
                val uri = Uri.parse(originalPath)
                contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it)
                }
            } else {
                BitmapFactory.decodeFile(originalPath)
            }
            
            withContext(Dispatchers.Main) {
                currentBitmap = bitmap
                photoView.setImageBitmap(bitmap)
            }
        }
    }

    private fun startCrop() {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val sourceUri = if (originalPath.startsWith("content://")) {
            Uri.parse(originalPath)
        } else {
            Uri.fromFile(File(originalPath))
        }
        
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(UCrop.Option.FREE)
            .withMaxResultSize(2048, 2048)
        
        cropLauncher.launch(uCrop.getIntent(this))
    }

    private fun rotateImage() {
        currentBitmap?.let { bitmap ->
            lifecycleScope.launch(Dispatchers.Default) {
                val rotated = rotateBitmap(bitmap, 90f)
                withContext(Dispatchers.Main) {
                    currentBitmap = rotated
                    photoView.setImageBitmap(rotated)
                }
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun showFilterDialog() {
        AlertDialog.Builder(this)
            .setTitle("选择滤镜")
            .setItems(filterNames.toTypedArray()) { _, which ->
                applyFilter(which)
            }
            .show()
    }

    private fun applyFilter(index: Int) {
        currentBitmap?.let { bitmap ->
            lifecycleScope.launch(Dispatchers.Default) {
                val matrix = filterMatrices[index]
                val colorMatrix = ColorMatrix(matrix)
                val filtered = applyColorMatrix(bitmap, colorMatrix)
                withContext(Dispatchers.Main) {
                    currentBitmap = filtered
                    photoView.setImageBitmap(filtered)
                    currentFilterIndex = index
                    Toast.makeText(this@ImageEditActivity, "已应用：${filterNames[index]}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyColorMatrix(bitmap: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val paint = android.graphics.Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }

    private fun showAdjustDialog() {
        val adjustments = arrayOf("亮度 +20", "对比度 +20", "饱和度 +20", "重置")
        AlertDialog.Builder(this)
            .setTitle("调节参数")
            .setItems(adjustments) { _, which ->
                when (which) {
                    0 -> adjustBrightness(20)
                    1 -> adjustContrast(1.2f)
                    2 -> adjustSaturation(1.2f)
                    3 -> resetAdjustments()
                }
            }
            .show()
    }

    private fun adjustBrightness(value: Int) {
        currentBitmap?.let { bitmap ->
            lifecycleScope.launch(Dispatchers.Default) {
                val adjusted = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
                val canvas = android.graphics.Canvas(adjusted)
                val paint = android.graphics.Paint()
                
                val brightnessMatrix = ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, value.toFloat(),
                    0f, 1f, 0f, 0f, value.toFloat(),
                    0f, 0f, 1f, 0f, value.toFloat(),
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(brightnessMatrix)
                
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                
                withContext(Dispatchers.Main) {
                    currentBitmap = adjusted
                    photoView.setImageBitmap(adjusted)
                    Toast.makeText(this@ImageEditActivity, "亮度已调整", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun adjustContrast(value: Float) {
        currentBitmap?.let { bitmap ->
            lifecycleScope.launch(Dispatchers.Default) {
                val adjusted = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
                val canvas = android.graphics.Canvas(adjusted)
                val paint = android.graphics.Paint()
                
                val contrastMatrix = ColorMatrix(floatArrayOf(
                    value, 0f, 0f, 0f, 0f,
                    0f, value, 0f, 0f, 0f,
                    0f, 0f, value, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
                
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                
                withContext(Dispatchers.Main) {
                    currentBitmap = adjusted
                    photoView.setImageBitmap(adjusted)
                    Toast.makeText(this@ImageEditActivity, "对比度已调整", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun adjustSaturation(value: Float) {
        currentBitmap?.let { bitmap ->
            lifecycleScope.launch(Dispatchers.Default) {
                val adjusted = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
                val canvas = android.graphics.Canvas(adjusted)
                val paint = android.graphics.Paint()
                
                val saturationMatrix = ColorMatrix()
                saturationMatrix.setSaturation(value)
                paint.colorFilter = ColorMatrixColorFilter(saturationMatrix)
                
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                
                withContext(Dispatchers.Main) {
                    currentBitmap = adjusted
                    photoView.setImageBitmap(adjusted)
                    Toast.makeText(this@ImageEditActivity, "饱和度已调整", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resetAdjustments() {
        currentBitmap = if (originalPath.startsWith("content://")) {
            val uri = Uri.parse(originalPath)
            contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it)
            }
        } else {
            BitmapFactory.decodeFile(originalPath)
        }
        photoView.setImageBitmap(currentBitmap)
        currentFilterIndex = -1
        Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show()
    }

    private fun saveImage() {
        currentBitmap?.let { bitmap ->
            lifecycleScope.launch(Dispatchers.IO) {
                val savedPath = saveBitmapToFile(bitmap)
                withContext(Dispatchers.Main) {
                    val intent = Intent().apply {
                        putExtra("edited_image_path", savedPath)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String {
        val dir = getExternalFilesDir("edited_images") ?: filesDir
        dir.mkdirs()
        
        val file = File(dir, "${System.currentTimeMillis()}_edited.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file.absolutePath
    }
}
