package com.example.syncmasterplus.data.chat.mapper

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}