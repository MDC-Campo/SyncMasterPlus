package com.example.syncmasterplus.presentation

import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothDeviceDomain
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothDevice
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val messages: List<BluetoothMessage> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    var peerDevice: BluetoothDeviceDomain? = null
)
