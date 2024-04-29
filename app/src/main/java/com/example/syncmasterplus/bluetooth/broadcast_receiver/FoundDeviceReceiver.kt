package com.example.syncmasterplus.bluetooth.broadcast_receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

//Receptor
class FoundDeviceReceiver (private val onDeviceFound: (BluetoothDevice)-> Unit):BroadcastReceiver() {
    //se invoca cuando recibe un intent
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Log.e("TAG", "FoundDeviceReceiver : yes TIRAMISU ")
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )
                } else {
                    Log.e("TAG", "FoundDeviceReceiver : yes ")
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE
                    )
                }
                device?.let(onDeviceFound)
            }
        }
    }
}