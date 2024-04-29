package com.example.syncmasterplus.domain.chat.playback

import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun start()
    fun stop()
    fun seekTo(position: Int)
    fun getAudioDuration():Int
    fun getCurrentPosition():Int
}