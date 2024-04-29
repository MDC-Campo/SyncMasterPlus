package com.example.syncmasterplus.data.chat.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.syncmasterplus.data.chat.models.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE sender_address = :senderAddress")
    fun getAllMessages(senderAddress: String): Flow<List<MessageEntity>>

    @Delete
    suspend fun deleteMessage(message: MessageEntity)
}