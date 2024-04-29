package com.example.syncmasterplus.data.chat.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.syncmasterplus.data.chat.models.MessageEntity
import com.example.syncmasterplus.data.chat.room.RoomDao

@Database(entities = [MessageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
}