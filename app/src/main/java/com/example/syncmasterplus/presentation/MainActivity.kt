package com.example.syncmasterplus.presentation

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.syncmasterplus.presentation.bluetooth_screen.BluetoothScreen
import com.example.syncmasterplus.presentation.components.PermissionDialog
import com.example.syncmasterplus.ui.theme.SyncMasterPlusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    //Comprueba si el bluetooth esta activado
    private val isBluetoothEnable: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("TAG", "main: running")

        //Launcher resultados de actividad para habilitar los Bluetooth
        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {}

        //Solicitar permisos
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            //Compruebe si se conceden permisos de Bluetooth
            val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[android.Manifest.permission.BLUETOOTH_CONNECT] == true

            } else true

            //Se otorgan permisos y Bluetooth no está habilitado, solicite habilitar Bluetooth
            if (canEnableBluetooth && !isBluetoothEnable) {
                enableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }

        //Solicitud de permiso de inicio basada en la versión de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.RECORD_AUDIO
                )
            )
        } else {

            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.RECORD_AUDIO

                )
            )
        }

        //Permisos de almacenamiento
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_AUDIO))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(arrayOf(READ_EXTERNAL_STORAGE))
        } else {
            permissionLauncher.launch(arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE))
        }

        //Hacer que el dispositivo sea visible para buscar otros dispositivos
        makeDeviceDiscoverable()

        setContent {
            SyncMasterPlusTheme {
                CheckAndRequestLocationPermission(enableBluetoothLauncher)
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    BluetoothScreen(
                        applicationContext
                    )

                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun makeDeviceDiscoverable() {
        if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            Toast.makeText(
                applicationContext,
                "Por favor habilite los permisos de bluetooth",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        //Launch Para que el dispositivo sea reconocible
        val requestCode = 1;
        val discoverableIntent: Intent =
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
        startActivityForResult(discoverableIntent, requestCode)

    }

    @Composable
    private fun CheckAndRequestLocationPermission(enableBluetoothLauncher: ActivityResultLauncher<Intent>) {
        var dialogOpen by remember { mutableStateOf(true) }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isLocationEnable(this)) {
            if (dialogOpen) {
                PermissionDialog(onOkClick = {
                    enableBluetoothLauncher.launch(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }, onDismissClick = {
                    dialogOpen = false
                })
            }
        }
    }

    //Comprueba si la aplicacion esta habilitada
    //context de la app
    //true habilitada | false deshabilitada

    fun isLocationEnable(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun hasPermission(permission: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return applicationContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }

}