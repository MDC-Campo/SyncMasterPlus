package com.example.syncmasterplus.domain.chat.recorder

import java.io.File

interface AudioRecord {
    fun start(outputFile: File)
    fun stop()
}