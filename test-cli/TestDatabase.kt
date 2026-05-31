import java.sql.DriverManager
import java.io.File
import java.util.Date
import java.util.UUID

// 简化的数据类
data class Category(
    val id: String,
    var name: String,
    var coverPath: String,
    val categoryType: String
)

data class MediaItem(
    val id: String,
    val localPath: String,
    var title: String,
    val mediaType: String
)

fun main() {
    println("=======================================")
    println("   Vivo 相册 - 数据库测试")
    println("=======================================\n")

    val dbPath = "/tmp/vivo_album_test.db"
    val dbFile = File(dbPath)
    if (dbFile.exists()) {
        dbFile.delete()
    }

    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")

    try {
        // 创建表
        println("[1] 创建数据库表...")
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    coverPath TEXT,
                    categoryType TEXT NOT NULL
                )
            """)

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS media_items (
                    id TEXT PRIMARY KEY,
                    localPath TEXT NOT NULL,
                    title TEXT NOT NULL,
                    mediaType TEXT NOT NULL
                )
            """)

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS category_media_join (
                    categoryId TEXT,
                    mediaId TEXT,
                    PRIMARY KEY (categoryId, mediaId)
                )
            """)
        }
        println("    ✓ 数据库表创建成功\n")

        // 插入默认分类
        println("[2] 插入默认分类...")
        val defaultCategories = listOf(
            Category(UUID.randomUUID().toString(), "所有照片", "", "album"),
            Category(UUID.randomUUID().toString(), "最近删除", "", "album"),
            Category(UUID.randomUUID().toString(), "我的漫画", "", "comic"),
            Category(UUID.randomUUID().toString(), "我的视频", "", "video")
        )

        conn.createStatement().use { stmt ->
            defaultCategories.forEach { cat ->
                stmt.executeUpdate("""
                    INSERT INTO categories (id, name, coverPath, categoryType)
                    VALUES ('${cat.id}', '${cat.name}', '${cat.coverPath}', '${cat.categoryType}')
                """)
            }
        }
        println("    ✓ 插入 ${defaultCategories.size} 个分类\n")

        // 查询并显示分类
        println("[3] 查询所有分类:")
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT * FROM categories ORDER BY categoryType, name")
            var count = 0
            while (rs.next()) {
                val id = rs.getString("id")
                val name = rs.getString("name")
                val type = rs.getString("categoryType")
                println("    - $name (类型：$type)")
                count++
            }
            println("    共 $count 个分类\n")
        }

        // 插入测试媒体项
        println("[4] 插入测试媒体项...")
        val mediaItems = listOf(
            MediaItem(UUID.randomUUID().toString(), "/photos/img001.jpg", "照片 1", "image"),
            MediaItem(UUID.randomUUID().toString(), "/photos/img002.jpg", "照片 2", "image"),
            MediaItem(UUID.randomUUID().toString(), "/comic/page001.jpg", "漫画第 1 页", "comic_page"),
            MediaItem(UUID.randomUUID().toString(), "/video/vid001.mp4", "视频 1", "video")
        )

        conn.createStatement().use { stmt ->
            mediaItems.forEach { media ->
                stmt.executeUpdate("""
                    INSERT INTO media_items (id, localPath, title, mediaType)
                    VALUES ('${media.id}', '${media.localPath}', '${media.title}', '${media.mediaType}')
                """)
            }
        }
        println("    ✓ 插入 ${mediaItems.size} 个媒体项\n")

        // 建立关联
        println("[5] 建立分类与媒体关联...")
        val albumCategory = defaultCategories.first { it.name == "所有照片" }
        val comicCategory = defaultCategories.first { it.name == "我的漫画" }
        val videoCategory = defaultCategories.first { it.name == "我的视频" }

        conn.createStatement().use { stmt ->
            // 所有照片包含所有图片和漫画
            stmt.executeUpdate("""
                INSERT INTO category_media_join (categoryId, mediaId)
                VALUES ('${albumCategory.id}', '${mediaItems[0].id}')
            """)
            stmt.executeUpdate("""
                INSERT INTO category_media_join (categoryId, mediaId)
                VALUES ('${albumCategory.id}', '${mediaItems[1].id}')
            """)
            stmt.executeUpdate("""
                INSERT INTO category_media_join (categoryId, mediaId)
                VALUES ('${albumCategory.id}', '${mediaItems[2].id}')
            """)

            // 我的漫画只包含漫画
            stmt.executeUpdate("""
                INSERT INTO category_media_join (categoryId, mediaId)
                VALUES ('${comicCategory.id}', '${mediaItems[2].id}')
            """)

            // 我的视频包含视频
            stmt.executeUpdate("""
                INSERT INTO category_media_join (categoryId, mediaId)
                VALUES ('${videoCategory.id}', '${mediaItems[3].id}')
            """)
        }
        println("    ✓ 关联建立成功\n")

        // 查询某个分类的媒体
        println("[6] 查询'所有照片'分类的媒体:")
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("""
                SELECT m.* FROM media_items m
                JOIN category_media_join j ON m.id = j.mediaId
                WHERE j.categoryId = '${albumCategory.id}'
            """)
            var count = 0
            while (rs.next()) {
                val title = rs.getString("title")
                val type = rs.getString("mediaType")
                println("    - $title ($type)")
                count++
            }
            println("    共 $count 个媒体项\n")
        }

        // 测试删除
        println("[7] 测试删除功能...")
        conn.createStatement().use { stmt ->
            stmt.executeUpdate("DELETE FROM media_items WHERE id = '${mediaItems[1].id}'")
        }
        println("    ✓ 删除'照片 2'成功\n")

        // 再次查询验证
        println("[8] 验证删除结果:")
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT COUNT(*) as count FROM media_items")
            rs.next()
            val count = rs.getInt("count")
            println("    剩余媒体项：$count 个")
        }

        println("\n=======================================")
        println("   所有测试通过！✓")
        println("=======================================")

    } catch (e: Exception) {
        println("\n❌ 错误：${e.message}")
        e.printStackTrace()
    } finally {
        conn.close()
        dbFile.delete()
        println("\n[清理] 临时数据库已删除")
    }
}
