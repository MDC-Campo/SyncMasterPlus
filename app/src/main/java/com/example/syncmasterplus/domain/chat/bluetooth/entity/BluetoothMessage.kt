package com.example.syncmasterplus.domain.chat.bluetooth.entity

import com.example.syncmasterplus.data.chat.models.MessageEntity
import com.example.syncmasterplus.data.chat.models.MessageType

sealed class BluetoothMessage {

    data class TextMessage(
        val text: String,
        val date: String,
        val time: String,
        val senderName: String,
        var senderAddress: String,
        val isFromLocalUser: Boolean
    ) : BluetoothMessage()

    data class ImageMessage(
        val imgData: ByteArray,
        val date: String,
        val time: String,
        val senderName: String,
        var senderAddress: String,
        val isFromLocalUser: Boolean
    ) : BluetoothMessage()

    data class AudioMessage(
        val audioData: ByteArray,
        val date: String,
        val time: String,
        val senderName: String,
        var senderAddress: String,
        val isFromLocalUser: Boolean
    ) : BluetoothMessage()

}

fun BluetoothMessage.toMessageEntity(path: String): MessageEntity {

    return when (this) {

        is BluetoothMessage.TextMessage -> {
            val msg = this
            MessageEntity().apply {
                type = MessageType.TEXT.value
                data = text
                date = msg.date
                time = msg.time
                senderName = msg.senderName
                senderAddress = msg.senderAddress
                isFromLocalUser = msg.isFromLocalUser
            }
        }

        is BluetoothMessage.AudioMessage -> {
            val msg = this
            MessageEntity().apply {
                type = MessageType.AUDIO.value
                data = path
                date = msg.date
                time = msg.time
                senderName = msg.senderName
                senderAddress = msg.senderAddress
                isFromLocalUser = msg.isFromLocalUser
            }
        }
        is BluetoothMessage.ImageMessage -> {
            val msg = this
            MessageEntity().apply {
                type = MessageType.IMAGE.value
                data = path
                date = msg.date
                time = msg.time
                senderName = msg.senderName
                senderAddress = msg.senderAddress
                isFromLocalUser = msg.isFromLocalUser
            }
        }
    }
}