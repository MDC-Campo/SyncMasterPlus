package com.example.syncmasterplus.domain.chat.bluetooth.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_devices")
data class BluetoothDevice(
    val name: String?,
    @PrimaryKey
    val address: String
)