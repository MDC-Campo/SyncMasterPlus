package com.example.syncmasterplus.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.syncmasterplus.bluetooth.broadcast_receiver.BluetoothStateReceiver
import com.example.syncmasterplus.bluetooth.broadcast_receiver.FoundDeviceReceiver
import com.example.syncmasterplus.data.chat.mapper.toBluetoothDeviceDomain
import com.example.syncmasterplus.data.chat.mapper.toByteArray
import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothController
import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothDeviceDomain
import com.example.syncmasterplus.domain.chat.bluetooth.ConnectionResult
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import com.example.syncmasterplus.domain.chat.util.Constants
import com.example.syncmasterplus.domain.chat.util.DateAndTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        //obtiene el adaptador si no es null
        bluetoothManager?.adapter
    }

    private var dataTransferService: BluetoothDataTransferService? = null

    //permite cambiar su valor desde multiples corrutinas
    private val _isConnected = MutableStateFlow(false)
    //asegura de que no pueda ser modificada desde fuera de la clase definida
    override val isConnected: StateFlow<Boolean>
        //convierte en un Stateflow asegurando
        get() = _isConnected.asStateFlow()


    private val _connectedDevice = MutableStateFlow(BluetoothDeviceDomain("", ""))
    override val connectedDevice: StateFlow<BluetoothDeviceDomain>
        get() = _connectedDevice.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()


    private val foundDeviceReceiver = FoundDeviceReceiver { device ->

        _scannedDevices.update { devices ->
            Log.d("TAG", "scanner : updated ")
            val newDevice = device.toBluetoothDeviceDomain()
            //Se modifica si el dispositivo no esta en la lista agregandolo
            if (newDevice in devices) devices else devices + newDevice

        }
    }


    private val bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        //si esta dentro de los dispositvimos emparejados actualiza el estado de la conexion
        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            _isConnected.update { isConnected }
            _connectedDevice.update { bluetoothDevice.toBluetoothDeviceDomain() }

        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _errors.emit("No se puede conectar a un dispositivo no emparejado")
            }
        }
    }

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    init {
        Log.e("TAG", "android controller: running...")
        //actualiza la lista de dispositvios
        updatePairedDevices()
        Log.e("TAG", "Registered  : bluetoothStateReceiver ")
        context.registerReceiver(
            //estados de conexion, desconexion y cambios de estado en el adaptador
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }


    override fun startDiscovery() {
        //verifica que tenga los permisos
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        ) {
            Toast.makeText(context, "Habilite los permisos de Bluetooth. ", Toast.LENGTH_SHORT)
                .show()
            Log.e("TAG", "startDiscovery(permission) scanner : No ")
            return
        }
        Log.e("TAG", "Registered  : foundDeviceReceiver ")
        //si tiene los permisos se registra el receptor de eventos para escuchar dispositivos de bluetooth
        //actualiza la lista de dispositivos emparejados
        //proceso de descubrimiento de  dispositvos
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Toast.makeText(context, "Por favor habilité los permisos de Bluetooth ", Toast.LENGTH_SHORT)
                .show()
            return
        }
        //si tiene los permisos habilitados, cancela el proceso de encontrar dispositivos bluetooth
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                Toast.makeText(context, "Por favor habilité los permisos de Bluetooth. ", Toast.LENGTH_SHORT)
                    .show()
            }

            //Se inicializa el adaptador y configuracion del servidor socket
            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "chat_service",
                UUID.fromString(SERVICE_UUID)
            )

            var shouldLoop = true
            //mientas que sea verdadera intenta aceptar una conexion entrante al servidor
            while (shouldLoop) {
                currentClientSocket = try {
                    currentServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                //emite un resultado de conexion (conectado)
                emit(ConnectionResult.ConnectionEstablished(_connectedDevice.value))
                currentClientSocket?.let {
                    currentServerSocket?.close()
                    //transferencia de datos
                    val service = BluetoothDataTransferService(it)
                    dataTransferService = service
                    try{
                        emitAll(
                            service
                                //escucha de mensajes
                                .listenForIncomingMessages(_connectedDevice.value.address)
                                .map {
                                    //emite resultado de transferencia de datos
                                    ConnectionResult.TransferSucceeded(it)
                                }
                        )
                    }catch (e: IOException){
                        Log.e("TAG", "startBluetoothServer: ${e.toString()}", )
                    }


                }
            }
        }.onCompletion {
            //cierra la conexion
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }


    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                Toast.makeText(context, "Por favor habilité los permisos de Bluetooth. ", Toast.LENGTH_SHORT)
                    .show()
            }
            //Establece la conexion bluetooth
            currentClientSocket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                //crea un socket RFCOMM para comunicarse con el servicio bluetooth(SERVICE_UUID)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
            //detiene el proceso de encontrar dispositivos bluetooth
            stopDiscovery()

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished(_connectedDevice.value))

                    BluetoothDataTransferService(socket).also {
                        dataTransferService = it
                        emitAll(
                            it.listenForIncomingMessages(_connectedDevice.value.address)

                                .map { bluetoothMessage ->

                                    ConnectionResult.TransferSucceeded(bluetoothMessage)
                                }
                        )
                    }
                } catch (e: IOException) {
                    Log.e("TAG", "connectToDevice: ${e.message}+++++ ${e.cause}")
                    socket.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Conexión interrumpida"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    @SuppressLint("HardwareIds")
    override suspend fun trySendMessage(message: String): BluetoothMessage? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Toast.makeText(context, "Por favor habilité los permisos de Bluetooth. ", Toast.LENGTH_SHORT)
                .show()
            return null
        }

        if (dataTransferService == null) {
            Toast.makeText(context, "Conecte un dispositivo e intente de nuevo.", Toast.LENGTH_SHORT)
                .show()
            return null
        }

        val bluetoothMessage = BluetoothMessage.TextMessage(
            text = message,
            senderName = bluetoothAdapter?.name ?: "Nombre desconocido",
            senderAddress = "",
            date = DateAndTime.getTodayDate(),
            time = DateAndTime.getCurrentTime(),
            //se establece como true para indicar que el mensaje proviene del usuario local
            isFromLocalUser = true
        )
        //convierte bluetoothMessage a un array de bytes
        //envia el mensaje
        dataTransferService?.sendMessage(bluetoothMessage.toByteArray().appendTextMarker())
        //establece la direccion del dispositivo conectado
        bluetoothMessage.senderAddress = _connectedDevice.value.address

        return bluetoothMessage
    }

    @SuppressLint("HardwareIds")
    override suspend fun trySendMessage(audioData: File): BluetoothMessage? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Toast.makeText(context, "Por favor habilité los permisos de Bluetooth. ", Toast.LENGTH_SHORT)
                .show()
            return null
        }


        if (dataTransferService == null) {
            Toast.makeText(context, "Conecte un dispositivo e intente de nuevo", Toast.LENGTH_SHORT)
                .show()
            return null
        }

        val bluetoothMessage = BluetoothMessage.AudioMessage(
            audioData = audioData.readBytes(),
            senderName = bluetoothAdapter?.name ?: "Nombre desconocido",
            senderAddress = "",
            date = DateAndTime.getTodayDate(),
            time = DateAndTime.getCurrentTime(),
            isFromLocalUser = true
        )

        dataTransferService?.sendMessage(bluetoothMessage.toByteArray().appendAudioMarker())
        bluetoothMessage.senderAddress = _connectedDevice.value.address
        Log.e("TAG", "dataTransferService  : ok")

        return bluetoothMessage
    }

    @SuppressLint("HardwareIds")
    override suspend fun trySendMessage(imageData:ByteArray): BluetoothMessage? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Toast.makeText(context, "Por favor habilité los permisos de Bluetooth", Toast.LENGTH_SHORT)
                .show()
            return null
        }

        if (dataTransferService == null) {
            Toast.makeText(context, "Conecte un dispositivo e intente de nuevo", Toast.LENGTH_SHORT)
                .show()
            return null
        }

        val bluetoothMessage = BluetoothMessage.ImageMessage(
            imgData = imageData,
            senderName = bluetoothAdapter?.name ?: "Nombre desconocido",
            senderAddress = "",
            date = DateAndTime.getTodayDate(),
            time = DateAndTime.getCurrentTime(),
            isFromLocalUser = true
        )

        dataTransferService?.sendMessage(bluetoothMessage.toByteArray().appendImageMarker())
        bluetoothMessage.senderAddress = _connectedDevice.value.address


        return bluetoothMessage
    }

    override fun closeConnection() {
        //verifica si no es nulo, llama al metodo close cerrando la conexion al cliente
        currentClientSocket?.close()
        //verifica si no es nulo, llama al metodo close cerrando la conexion al servidor
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
        //  release()
    }

    //cierra las conexiones, deregistra los receptores found y state
    override fun release() {
        try {
            context.unregisterReceiver(foundDeviceReceiver)
            context.unregisterReceiver(bluetoothStateReceiver)
            closeConnection()
        } catch (e: Exception) {
            Log.e("TAG", "release: $e")
        }
    }

    //actualiza la lista de dispositivos emparejados
    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Toast.makeText(context, "Por favor habilité los permisos de Bluetooth. ", Toast.LENGTH_SHORT)
                .show()
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.filter {
                //solo filtra los dispositivos emparejados PhoneSmart
                it.bluetoothClass.deviceClass == BluetoothClass.Device.PHONE_SMART
            }
            ?.map {
                it.toBluetoothDeviceDomain()
            }
            ?.also { devices ->
                _pairedDevices.update { devices }
            }
    }

    //Verifica si la version android es >= api 31 (android 12)
    //verifica permisos
    private fun hasPermission(permission: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

    //identificador unico, conexion bluetooth
    companion object {
        const val SERVICE_UUID = "27b7d1da-08c7-4505-a6d1-2459987e5e2d"
    }

}

//agrega el byte marcador a una matriz de bytez
fun ByteArray.appendTextMarker(): ByteArray {
    return this + byteArrayOf(Constants.TEXT_MSG_MARK)
}
//agrega el byte marcador a una matriz de bytez
fun ByteArray.appendAudioMarker(): ByteArray {
    return this + byteArrayOf(Constants.AUDIO_MSG_MARK)
}

fun ByteArray.appendImageMarker(): ByteArray {
    //byte array vacio(Constants.IMAGE_MSG_MARK) + datos originales(this)...
    return byteArrayOf(Constants.IMAGE_MSG_MARK)+ this +byteArrayOf(Constants.IMAGE_MSG_MARK2)+ byteArrayOf(
        Constants.IMAGE_MSG_MARK)
}
