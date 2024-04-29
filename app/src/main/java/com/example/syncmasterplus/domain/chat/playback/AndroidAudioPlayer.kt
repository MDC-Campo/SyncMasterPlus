package com.example.syncmasterplus.domain.chat.playback

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.core.net.toUri
import java.io.File

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    private var player: MediaPlayer? = null

    override fun playFile(file: File) {
        MediaPlayer.create(context, file.toUri()).apply {
            player = this
        }
    }

    override fun start() {
        player?.start()
    }

    override fun stop() {
        player?.pause()
    }

    override fun seekTo(position: Int) {

        try {
            player?.seekTo(position)
        } catch (e: Exception) {
            Log.e("TAG", "seekTo: ${e.toString()}")
        }

    }

    override fun getAudioDuration(): Int {
        return player?.duration!!
    }

    override fun getCurrentPosition(): Int {
        return player?.currentPosition!!
    }
}