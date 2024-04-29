package com.example.syncmasterplus.data.chat.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.UUID

class ExternalStorage(private  val context: Context) {

    //permite acceder a los datos de contenido
    private val contentResolver: ContentResolver
        get() = context.contentResolver


    fun saveAudioFile(name:String , audioMessage: BluetoothMessage.AudioMessage):String {

        //almacenar archivos
        val audioCollection = sdk29AndUp {
            //sdk 29 o superior
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            //versiones anterios a 29
        } ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            //se especifican los metadatos
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$name.mp3")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
        }
        //se insert y guarda archivos
        //si cumple guarda el archivo con el nombre del archivo
        return try {
            contentResolver.insert(audioCollection, values)?.also { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(audioMessage.audioData)
                }

            }
            "$name.mp3"
        }catch (e: IOException){
            e.printStackTrace()
            ""
        }
    }

    fun saveImageFile(name:String , imageMessage: BluetoothMessage.ImageMessage):String {

        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI


        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }


        return try {
            contentResolver.insert(imageCollection, values)?.also { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(imageMessage.imgData)
                }

            }
            "$name.jpg"
        }catch (e:IOException){
            Log.e("IMAGE", "saveImageFile: ${e.toString()}", )
            e.printStackTrace()
            ""
        }
    }
    fun retrieveAudioFile(fileName: String): ByteArray? {
        val audioCollection = sdk29AndUp {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(MediaStore.Audio.Media._ID)

        // Query for the audio file with the specified display name
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        contentResolver.query(audioCollection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // Retrieve the audio file's URI
                val audioUri = Uri.withAppendedPath(audioCollection, cursor.getLong(0).toString())
                // Read the audio file into a byte array
                return contentResolver.openInputStream(audioUri)?.use { inputStream ->
                    val buffer = ByteArrayOutputStream()
                    inputStream.copyTo(buffer)
                    buffer.toByteArray()
                }
            }
        } ?: return null

        return null
    }

    fun retrieveImageFile(fileName: String): ByteArray? {
        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(MediaStore.Images.Media._ID)

        // Query for the audio file with the specified display name
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        contentResolver.query(imageCollection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // Retrieve the audio file's URI
                val imageUri = Uri.withAppendedPath(imageCollection, cursor.getLong(0).toString())
                // Read the audio file into a byte array
                return contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    val buffer = ByteArrayOutputStream()
                    inputStream.copyTo(buffer)
                    buffer.toByteArray()
                }
            }
        } ?: return null

        return null
    }

    fun exportChatToText(messages : List<BluetoothMessage>) : String{

        val dir = createTextFileDirectory()


        if (dir != null) {
            val file = File(dir, "${UUID.randomUUID()}.txt")
            try {
                FileWriter(file).use { writer ->
                    messages.forEach { message ->
                        var sender = ""
                        val line = when (message) {
                            is BluetoothMessage.TextMessage -> {
                                sender =   if(message.isFromLocalUser) "You" else message.senderName
                                "${sender} @ ${message.date} ${message.time}: ${message.text}\n"
                            }
                            is BluetoothMessage.AudioMessage -> {
                                sender =   if(message.isFromLocalUser) "You" else message.senderName
                                "${sender} @ ${message.date} ${message.time}: Audio Message\n" // You can customize this based on your requirements
                            }
                            is BluetoothMessage.ImageMessage -> {
                                sender =   if(message.isFromLocalUser) "You" else message.senderName
                                "${sender} @ ${message.date} ${message.time}: Image Message\n" // You can customize this based on your requirements
                            }
                        }
                        writer.append(line)
                    }
                    writer.flush()
                    writer.close()
                }
                return file.absolutePath
            } catch (e: IOException) {
                Log.e("SaveTextFile", "Error writing text to file: ${e.message}")
            }
        } else {
            Log.e("SaveTextFile", "Error creating text file directory")
        }
        return ""
    }


    private fun createTextFileDirectory(): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloadsDir, "SyncMasterChat")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}