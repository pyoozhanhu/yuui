package com.vivo.album

import com.vivo.album.data.AppDatabase
import com.vivo.album.data.Category
import com.vivo.album.data.MediaItem
import com.vivo.album.data.repository.AlbumRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class AlbumRepositoryTest {

    private lateinit var repository: AlbumRepository
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = AppDatabase.getInstance(context)
        repository = AlbumRepository(context)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testDatabaseCreation() {
        println("=== 测试数据库创建 ===")
        assertNotNull(database, "数据库应该成功创建")
        println("数据库创建成功 ✓")
    }

    @Test
    fun testAddCategory() {
        println("\n=== 测试添加分类 ===")
        val category = Category(
            id = "test-1",
            name = "测试相册",
            coverPath = "/path/to/cover.jpg",
            categoryType = "album"
        )
        
        // 直接插入数据库
        database.categoryDao().insertCategory(category)
        
        val categories = database.categoryDao().getAllCategories()
        assertEquals(1, categories.size, "应该有一个分类")
        assertEquals("测试相册", categories[0].name)
        println("添加分类成功 ✓")
    }

    @Test
    fun testAddMediaItem() {
        println("\n=== 测试添加媒体项 ===")
        val mediaItem = MediaItem(
            id = "media-1",
            localPath = "/path/to/photo.jpg",
            title = "测试照片",
            tags = listOf("test", "photo"),
            favorite = true,
            mediaType = "image"
        )
        
        database.mediaItemDao().insertMediaItem(mediaItem)
        
        val allMedia = database.mediaItemDao().getAllMediaItems()
        assertEquals(1, allMedia.size, "应该有一个媒体项")
        assertEquals("测试照片", allMedia[0].title)
        println("添加媒体项成功 ✓")
    }

    @Test
    fun testCategoryMediaRelationship() {
        println("\n=== 测试分类与媒体关联 ===")
        
        // 创建分类
        val category = Category(
            id = "cat-1",
            name = "我的相册",
            coverPath = "",
            categoryType = "album"
        )
        database.categoryDao().insertCategory(category)
        
        // 创建媒体项
        val mediaItem = MediaItem(
            id = "media-1",
            localPath = "/path/to/photo.jpg",
            title = "照片",
            tags = listOf(),
            mediaType = "image"
        )
        database.mediaItemDao().insertMediaItem(mediaItem)
        
        // 建立关联
        val join = com.vivo.album.data.CategoryMediaJoin(
            categoryId = "cat-1",
            mediaId = "media-1"
        )
        database.joinDao().insert(join)
        
        // 验证关联
        val mediaForCategory = database.mediaItemDao().getMediaItemsForCategory("cat-1")
        assertEquals(1, mediaForCategory.size, "分类应该有一个媒体项")
        assertEquals("media-1", mediaForCategory[0].id)
        println("分类与媒体关联成功 ✓")
    }

    @Test
    fun testDeleteCategory() {
        println("\n=== 测试删除分类 ===")
        
        val category = Category(
            id = "cat-to-delete",
            name = "待删除",
            coverPath = "",
            categoryType = "album"
        )
        database.categoryDao().insertCategory(category)
        
        // 验证存在
        var categories = database.categoryDao().getAllCategories()
        assertTrue(categories.any { it.id == "cat-to-delete" }, "分类应该存在")
        
        // 删除
        database.categoryDao().deleteCategory(category)
        
        // 验证已删除
        categories = database.categoryDao().getAllCategories()
        assertTrue(categories.none { it.id == "cat-to-delete" }, "分类应该已删除")
        println("删除分类成功 ✓")
    }

    @Test
    fun testDefaultCategoriesInitialization() {
        println("\n=== 测试默认分类初始化 ===")
        
        // 先清空所有数据
        database.clearAllTables()
        
        // 模拟首次启动
        val allCategories = database.categoryDao().getAllCategories()
        if (allCategories.isEmpty()) {
            repository.addCategory("所有照片", "album")
            repository.addCategory("最近删除", "album")
            repository.addCategory("我的漫画", "comic")
            repository.addCategory("我的视频", "video")
        }
        
        val categories = database.categoryDao().getAllCategories()
        assertEquals(4, categories.size, "应该有 4 个默认分类")
        
        println("默认分类列表:")
        categories.forEach {
            println("  - ${it.name} (类型：${it.categoryType})")
        }
        
        assertTrue(categories.any { it.name == "所有照片" })
        assertTrue(categories.any { it.name == "最近删除" })
        assertTrue(categories.any { it.name == "我的漫画" })
        assertTrue(categories.any { it.name == "我的视频" })
        println("默认分类初始化成功 ✓")
    }
}
