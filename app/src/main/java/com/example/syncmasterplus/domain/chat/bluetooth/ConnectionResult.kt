package com.example.syncmasterplus.domain.chat.bluetooth

import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage

sealed interface ConnectionResult {

    data class ConnectionEstablished(val peerDevice: BluetoothDeviceDomain): ConnectionResult

    data class TransferSucceeded(val message: BluetoothMessage): ConnectionResult

    data class Error(val message: String): ConnectionResult
}