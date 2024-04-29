package com.example.syncmasterplus.data.chat.models

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.syncmasterplus.data.chat.storage.ExternalStorage
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import java.util.UUID

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val _id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "type")var type: Int = MessageType.DEFAULT.value,
    @ColumnInfo(name = "data")var data: String = "",
    @ColumnInfo(name = "date")var date: String = "",
    @ColumnInfo(name = "time")var time: String = "",
    @ColumnInfo(name = "sender_name")var senderName: String = "",
    @ColumnInfo(name = "sender_address")var senderAddress: String = "",
    @ColumnInfo(name = "is_from_local_user")var isFromLocalUser: Boolean = true
)

enum class MessageType(val value: Int) {
    DEFAULT(0),
    TEXT(1),
    AUDIO(2),
    IMAGE(3)
}

fun MessageEntity.toBluetoothMessage(externalStorage: ExternalStorage): BluetoothMessage {
    return when (type) {
        MessageType.TEXT.value -> {
            BluetoothMessage.TextMessage(
                text = data,
                date = date,
                time = time,
                senderName = senderName,
                senderAddress = senderAddress,
                isFromLocalUser = isFromLocalUser
            )
        }
        MessageType.AUDIO.value -> {
            Log.e("TAAG", "toBluetoothMessage:AudioMessage--${this.data} ", )
            BluetoothMessage.AudioMessage(
                audioData = externalStorage.retrieveAudioFile(data)?: ByteArray(0),
                date = date,
                time = time,
                senderName = senderName,
                senderAddress = senderAddress,
                isFromLocalUser = isFromLocalUser
            )
        }
        MessageType.IMAGE.value -> {
            BluetoothMessage.ImageMessage(
                imgData = externalStorage.retrieveImageFile(data)?: ByteArray(0),
                date = date,
                time = time,
                senderName = senderName,
                senderAddress = senderAddress,
                isFromLocalUser = isFromLocalUser
            )
        }
        else -> {
         //Si el tipo de mensaje no coincide con ningun tipo conocido | lanza exception |
            BluetoothMessage.TextMessage(
                text = "Tipo de mensaje desconocido",
                date = date,
                time = time,
                senderName = senderName,
                senderAddress = senderAddress,
                isFromLocalUser = isFromLocalUser
            )
        }
    }
}