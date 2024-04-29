package com.example.syncmasterplus.data.chat.mapper

import android.util.Log
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

//convierte una cadena de texto en un objeto
fun String.toBluetoothMessage(isFromLocalUser: Boolean, senderAddress:String): BluetoothMessage {
    //split divide la cadena original utilizando #@
    //devuelve una lista String representando un campo
    val (senderName, _, date, time, message) = split("#@")
        //elimina cualquier concurrencia #@
        //Se asigna a las variables senderName, date time y message
        .map { it.replace("#@","") }
    return BluetoothMessage.TextMessage(
        text = message,
        senderName = senderName,
        senderAddress = senderAddress,
        date = date,
        time = time,
        isFromLocalUser = isFromLocalUser
    )
}

//convierte el objeto texto en un array de bytes
fun BluetoothMessage.TextMessage.toByteArray(): ByteArray {
    //formateando los campos y separandolos con el delimitador
    return "$senderName#@$senderAddress#@$date#@$time#@$text".encodeToByteArray()
}

//convierte array de bytes en un obj audio message
fun ByteArray.toBluetoothAudioMessage(isFromLocalUser: Boolean, senderAddress:String): BluetoothMessage {
    val inputStream = ByteArrayInputStream(this)
    val dataInputStream = DataInputStream(inputStream)

    // utf para leer las cadenas
    //lee senderName, senderAddres,date,time,
    val senderName = dataInputStream.readUTF()
    val senderAdd_ = dataInputStream.readUTF()
    val date = dataInputStream.readUTF()
    val time = dataInputStream.readUTF()
    // lee la longitud de los datos de audio
    val audioLength = dataInputStream.readInt()
    //lee los datos del audio
    val audioBytes = ByteArray(audioLength)
    dataInputStream.read(audioBytes)
     Log.e("TAG", "toBluetoothAudioMessage: ${audioBytes.contentToString()}-----${audioBytes.size}")
    // al finalizar de leer datos, cierra
    dataInputStream.close()

    return BluetoothMessage.AudioMessage(
        audioData = audioBytes,
        senderName = senderName,
        senderAddress = senderAddress,
        date = date,
        time = time,
        isFromLocalUser = isFromLocalUser
    )
}

//convierte un obj Audiomessage  en un byteArray
fun BluetoothMessage.AudioMessage.toByteArray(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val dataOutputStream = DataOutputStream(outputStream)

    // Escribe senderName, senderAddres, date, time.
    dataOutputStream.writeUTF(senderName)
    dataOutputStream.writeUTF(senderAddress)
    dataOutputStream.writeUTF(date)
    dataOutputStream.writeUTF(time)
    // Escribe la longitud de los datos de audio
    dataOutputStream.writeInt(audioData.size)
    // Escribe los datos de audio
    dataOutputStream.write(audioData)

    Log.e("TAG", "toByteArray: ${audioData.contentToString()}-----${audioData.size}")
    //asegura los datos de salida se escriban correctamente
    dataOutputStream.flush()
    //cierra para la liberacion de recursos
    dataOutputStream.close()
    //devuelve un byteArray
    return outputStream.toByteArray()
}
//convierte un objImage a un byteArray
fun BluetoothMessage.ImageMessage.toByteArray(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val dataOutputStream = DataOutputStream(outputStream)

   //Escribe senderName, senderAddres, date, time.
    dataOutputStream.writeUTF(senderName)
    dataOutputStream.writeUTF(senderAddress)
    dataOutputStream.writeUTF(date)
    dataOutputStream.writeUTF(time)
    // Escribe la longitud de los datos de img
    dataOutputStream.writeInt(imgData.size)
    // Escribe los datos de imagen
    dataOutputStream.write(imgData)
    Log.e("TAG", "toByteArray: ${imgData.contentToString()}-----${imgData.size}")
    //asegura los datos de salida se escriban correctamente
    dataOutputStream.flush()
    dataOutputStream.close()

    return outputStream.toByteArray()
}

//convierte un byteArray a un objImage
fun ByteArray.toBluetoothImageMessage(isFromLocalUser: Boolean, senderAddress:String): BluetoothMessage {
    val inputStream = ByteArrayInputStream(this)
    val dataInputStream = DataInputStream(inputStream)

    //lee senderName, senderAddres,date,time,
    val senderName = dataInputStream.readUTF()
    val senderAddres= dataInputStream.readUTF()
    val date = dataInputStream.readUTF()
    val time = dataInputStream.readUTF()
    // lee la longitud de los datos de audio
    val imageLength = dataInputStream.readInt()
    // lee los datos del audio
    val imageBytes = ByteArray(imageLength)

    dataInputStream.read(imageBytes)
    Log.e("TAG", "toBluetoothImageMessage: ${imageBytes.contentToString()}-----${imageBytes.size}")
    dataInputStream.close()

    return BluetoothMessage.ImageMessage(

        imgData = imageBytes,
        senderName = senderName,
        senderAddress = senderAddress,
        date = date,
        time = time,
        isFromLocalUser = isFromLocalUser
    )
}