package com.example.syncmasterplus.domain.chat.util

class Constants {
    companion object{
        const val TEXT_MSG_MARK:Byte = 0x7E
        const val AUDIO_MSG_MARK:Byte = 0x5E
        const val IMAGE_MSG_MARK: Byte = 0xFA.toByte()
        const val IMAGE_MSG_MARK2: Byte = 0xCD.toByte()
    }
}