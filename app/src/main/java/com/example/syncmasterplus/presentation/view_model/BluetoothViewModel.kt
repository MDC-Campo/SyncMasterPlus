package com.example.syncmasterplus.presentation.view_model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.syncmasterplus.data.chat.models.MessageEntity
import com.example.syncmasterplus.data.chat.models.toBluetoothMessage
import com.example.syncmasterplus.data.chat.room.RoomDao
import com.example.syncmasterplus.data.chat.storage.ExternalStorage
import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothController
import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothDeviceDomain
import com.example.syncmasterplus.domain.chat.bluetooth.ConnectionResult
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import com.example.syncmasterplus.domain.chat.bluetooth.entity.toMessageEntity
import com.example.syncmasterplus.domain.chat.playback.AndroidAudioPlayer
import com.example.syncmasterplus.domain.chat.recorder.AndroidAudioRecorder
import com.example.syncmasterplus.presentation.BluetoothUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    @Singleton
    private val bluetoothController: BluetoothController,
    private val audioPlayer: AndroidAudioPlayer,
    private val audioRecorder: AndroidAudioRecorder,
    private val cacheDir: File,
    private val roomDao: RoomDao,
    private val externalStorage: ExternalStorage
) : ViewModel() {

    private var audioFile: File? = null
    private var messageEntityList = mutableListOf<MessageEntity>()

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ){
            scannedDevices, pairedDevices, state ->
        Log.e("TAG", "scanning: running...")
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    //mutable
    private var deviceConnectionJob: Job? = null

    init {
        Log.e("TAG", "model: running")
        bluetoothController.isConnected.onEach { isConnected ->
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _state.update {
                it.copy(
                    errorMessage = error
                )
            }
        }.launchIn(viewModelScope)
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {

        _state.update { it.copy(isConnecting = true, peerDevice = device) }
        deviceConnectionJob = bluetoothController
            .connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update {
            it.copy(
                messages = emptyList(),
                isConnecting = false,
                isConnected = false
            )
        }
    }

    fun waitForIncomingConnections() {
        Log.d("TAG", "BluetoothServer  : STARTED ")
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController
            .startBluetoothServer()
            .listen()
    }

    fun sendMessage(message: String) {

        viewModelScope.launch {
            val bluetoothMessage = bluetoothController.trySendMessage(message)
            if (bluetoothMessage != null) {
                _state.update {
                    it.copy(
                        messages = it.messages + bluetoothMessage
                    )
                }
                insertMessage(bluetoothMessage.toMessageEntity(""))
            }
        }
    }

    fun sendMessage() {

        viewModelScope.launch {
            val bluetoothMessage = audioFile?.let { bluetoothController.trySendMessage(it) }
            if (bluetoothMessage != null) {
                _state.update {
                    it.copy(
                        messages = it.messages + bluetoothMessage
                    )
                }
                val path =
                    when(bluetoothMessage){
                        is BluetoothMessage.AudioMessage -> { externalStorage.saveAudioFile(UUID.randomUUID().toString(),bluetoothMessage)}
                        is BluetoothMessage.ImageMessage -> {""}
                        is BluetoothMessage.TextMessage -> {""}
                    }
                Log.e("TAGG", "saveAudioFile: $path saved", )
                insertMessage(bluetoothMessage.toMessageEntity(path))
            }
        }
    }

    fun sendMessage(imageData: ByteArray) {

        viewModelScope.launch {
            val bluetoothMessage =  bluetoothController.trySendMessage(imageData)
            if (bluetoothMessage != null) {

                _state.update {
                    it.copy(
                        messages = it.messages + bluetoothMessage
                    )
                }
                val path =
                    when(bluetoothMessage){
                        is BluetoothMessage.AudioMessage -> {""}
                        is BluetoothMessage.ImageMessage -> { externalStorage.saveImageFile(UUID.randomUUID().toString(),bluetoothMessage)

                        }
                        is BluetoothMessage.TextMessage -> {""}
                    }
                Log.e("IMAGE", "saveImageFile: $path saved", )
                insertMessage(bluetoothMessage.toMessageEntity(path))
            }
        }
    }

    fun startScan() {
        Log.d("TAG", "startScan  : started ")
        bluetoothController.startDiscovery()
    }

    fun stopScan() {
        Log.d("TAG", "stopScan  : stopped ")
        bluetoothController.stopDiscovery()
    }

    private fun Flow<ConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                is ConnectionResult.ConnectionEstablished -> {
                    _state.update {
                        it.copy(
                            peerDevice = result.peerDevice,
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null

                        )
                    }
                    loadOldMessages(result.peerDevice)
                }

                is ConnectionResult.TransferSucceeded -> {
                    _state.update {
                        it.copy(
                            messages = it.messages + result.message
                        )
                    }
                    when(result.message){
                        is BluetoothMessage.AudioMessage -> {
                            val path = externalStorage.saveAudioFile(UUID.randomUUID().toString(),result.message)
                            Log.e("TAAG", "listen: $path", )
                            if(path.isNotEmpty())
                                insertMessage(result.message.toMessageEntity(path))
                        }
                        is BluetoothMessage.ImageMessage -> {
                            val path = externalStorage.saveImageFile(UUID.randomUUID().toString(),result.message)
                            if(path.isNotEmpty())
                                insertMessage(result.message.toMessageEntity(path))
                        }
                        is BluetoothMessage.TextMessage -> insertMessage(result.message.toMessageEntity(""))
                    }


                }

                is ConnectionResult.Error -> {
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.message
                        )
                    }
                }

                else -> {}
            }
        }
            .catch { throwable ->
                bluetoothController.closeConnection()
                _state.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun releaseResources() = bluetoothController.release()

    //audio

    fun startRecord() {
        audioFile?.let { audioRecorder.start(it) }
    }

    fun stopRecord() {
        audioRecorder.stop()
        // setPlayer()
    }

    fun setPlayer(file: File){
        file.let { audioPlayer.playFile(it) }
    }

    fun startPlay() {
        audioPlayer.start()
    }

    fun stopPlay() {
        audioPlayer.stop()
    }

    fun seekTo(postion: Int) {
        audioPlayer.seekTo(postion)
    }

    fun getAudioDuration():Int {
        return audioPlayer.getAudioDuration()
    }

    fun getCurrentPosition():Int {
        return audioPlayer.getCurrentPosition()
    }

    fun createAudioFile(name: String) :File{
        return File(cacheDir,name).also {
            audioFile = it
        }
    }

    fun saveByteArrayToFile(audioData: ByteArray, file:File){
        try {
            val outputStream = FileOutputStream(file)
            outputStream.write(audioData)
            outputStream.close()
            Log.e("TAG","Audio data saved successfully to file: ${file?.absolutePath}")
        } catch (e: IOException) {
            println("Error saving audio data to file: ${e.message}")
        }
    }

    //DAO

    fun insertMessage(messageEntity: MessageEntity){
        Log.e("TAG", "insertMessage: ${messageEntity.date}--${messageEntity.senderAddress}--")
        viewModelScope.launch(Dispatchers.IO) {
            roomDao.insertMessage(messageEntity)
        }
    }

    fun getOldMessages(senderAddress:String): Flow<List<MessageEntity>> {
        return roomDao.getAllMessages(senderAddress)
    }

    fun loadOldMessages(device: BluetoothDeviceDomain){
        var bluetoothMessages: List<BluetoothMessage>
        viewModelScope.launch(Dispatchers.IO) {
            getOldMessages(device.address).collect { messages ->
                messageEntityList = messages.reversed().toMutableList()
                bluetoothMessages = messages.map { it.toBluetoothMessage(externalStorage)
                }
                _state.update {
                    it.copy(messages = bluetoothMessages.reversed(),
                        peerDevice = device)
                }
            }

        }

    }

    fun exportChatToTextFile(messages: List<BluetoothMessage>) : String{
        return externalStorage.exportChatToText(messages)
    }

    fun deleteMessage(message: BluetoothMessage){
        val position = _state.value.messages.indexOf(message)
        val messageEntity = messageEntityList.get(position)
        viewModelScope.launch {
            roomDao.deleteMessage(messageEntity)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cacheDir.deleteOnExit()
        //   bluetoothController.release()
        Log.e("TAG", "Viewmodel  : cleared ")
    }

}