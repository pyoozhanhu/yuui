package com.vivo.album.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vivo.album.data.dao.CategoryDao
import com.vivo.album.data.dao.JoinDao
import com.vivo.album.data.dao.MediaItemDao

@Database(entities = [Category::class, MediaItem::class, CategoryMediaJoin::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun joinDao(): JoinDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vivo_album_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_items ADD COLUMN originalCategoryId TEXT")
                database.execSQL("ALTER TABLE media_items ADD COLUMN mediaType TEXT DEFAULT 'image'")
                database.execSQL("ALTER TABLE media_items ADD COLUMN duration INTEGER")
            }
        }
    }
}
