package com.example.syncmasterplus.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.syncmasterplus.data.chat.mapper.toBluetoothAudioMessage
import com.example.syncmasterplus.data.chat.mapper.toBluetoothImageMessage
import com.example.syncmasterplus.data.chat.mapper.toBluetoothMessage
import com.example.syncmasterplus.domain.chat.bluetooth.TransferFailedException
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import com.example.syncmasterplus.domain.chat.util.Constants.Companion.AUDIO_MSG_MARK
import com.example.syncmasterplus.domain.chat.util.Constants.Companion.IMAGE_MSG_MARK
import com.example.syncmasterplus.domain.chat.util.Constants.Companion.IMAGE_MSG_MARK2
import com.example.syncmasterplus.domain.chat.util.Constants.Companion.TEXT_MSG_MARK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class BluetoothDataTransferService(private val socket: BluetoothSocket) {
    fun listenForIncomingMessages(senderAddress: String): Flow<BluetoothMessage> {
        return flow {
            if (!socket.isConnected) {
                return@flow
            }
            Log.d("TAG", "listenForIncomingMessages ")
            //lista para almacenar bloque de datos socket
            val bufferList = mutableListOf<ByteArray>()
            var buffer = ByteArray(990)
            //primer byte mensaje actual
            var firstByte: Byte? = null
            //penultimo byte menseaje actual
            var secondLastByte: Byte? = null
            var msg = ""
            while (true) {
                //byte leidos almacenados
                val byteCount = try {
                    //datos leidos almacenados
                    socket.inputStream.read(buffer)
                } catch (e: IOException) {
                    Log.d("TAG", "transefer failed Error: $e+++++ ${e.cause}")
                    throw TransferFailedException()
                }
                bufferList.add(buffer.copyOfRange(0, byteCount))
                if (firstByte == null) {
                    firstByte = buffer[0]
                }

                val lastByte = buffer[byteCount - 1]
                if (buffer.size > 1) {
                    secondLastByte = buffer[byteCount - 2]
                } else {
                    bufferList.lastOrNull()?.let { lastBuffer ->
                        secondLastByte = lastBuffer[lastBuffer.size - 1]
                    }
                }

                //Mensaje de texto recibido
                if (lastByte == TEXT_MSG_MARK && firstByte != IMAGE_MSG_MARK) {
                    Log.d("TAG", "TEXT MESSAGE RECEIVED")
                    Log.d("TAG", "lastbyte : ${lastByte.toString()}")
                    Log.d("TAG", "IMAGE_MSG_MARK : ${IMAGE_MSG_MARK.toString()}")
                    val combinedByteArray = bufferList.fold(ByteArray(0)) { acc, byteArray ->
                        acc + byteArray
                    }
                    val message = combinedByteArray.dropLast(1).toByteArray().decodeToString()

                    emit(
                        message.toBluetoothMessage(
                            isFromLocalUser = false, senderAddress
                        )

                    )
                    firstByte = null
                    bufferList.clear()

                } else if (lastByte == AUDIO_MSG_MARK && firstByte != IMAGE_MSG_MARK) {
                    Log.d("TAG", "AUDIO MESSAGE RECEIVED")
                    val combinedByteArray = bufferList.fold(ByteArray(0)) { acc, byteArray ->
                        acc + byteArray
                    }
                    emit(
                        combinedByteArray.copyOf(combinedByteArray.size).toBluetoothAudioMessage(
                            false, senderAddress
                        )
                    )
                    firstByte = null
                    bufferList.clear()

                    //mensaje de imagen  recibido
                } else if (lastByte == IMAGE_MSG_MARK && firstByte == IMAGE_MSG_MARK && secondLastByte == IMAGE_MSG_MARK2) {
                    Log.d("TAG", "IMAGE MESSAGE RECEIVED")
                    val combinedByteArray = bufferList.fold(ByteArray(0)) { acc, byteArray ->
                        acc + byteArray
                    }
                    val imageByteArray =
                        combinedByteArray.copyOfRange(1, combinedByteArray.size - 1)
                    emit(
                        imageByteArray.toBluetoothImageMessage(
                            false, senderAddress
                        )
                    )
                    //restablece los valores y limpia bufferList
                    //para recibir el siguiente mensaje
                    firstByte = null
                    secondLastByte = null
                    bufferList.clear()
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(bytes: ByteArray): Boolean {

        return withContext(Dispatchers.IO) {
            try {
                //escribe la el array de bytes
                socket.outputStream.write(bytes)
                Log.e("TAG", "sendMessage:bytecount: ${bytes}--${bytes}")
            } catch (e: IOException) {
                e.printStackTrace()
                //retorna un false indicando que no fue exitoso
                return@withContext false
            }
            true
        }
    }
}