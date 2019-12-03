package ru.vinyarsky.englishmedia.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.vinyarsky.englishmedia.models.data.db.PodcastEntity

@Database(entities = arrayOf(PodcastEntity::class), version = 2)
abstract class EMDatabase : RoomDatabase() {

    abstract fun getPoscastDao(): PodcastDao

    companion object {

        fun getInstance(context: Context): EMDatabase {
            if (INSTANCE == null) {
                synchronized(EMDatabase::class) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                                context.applicationContext,
                                EMDatabase::class.java,
                                "ru/vinyarsky/englishmedia/data")
                                .build()
                    }
                }
            }
            return INSTANCE
        }

        private lateinit var INSTANCE: EMDatabase
    }
}