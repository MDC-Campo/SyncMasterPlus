package com.example.syncmasterplus.presentation.device_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothDeviceDomain
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothDevice
import com.example.syncmasterplus.presentation.BluetoothUiState
import com.example.syncmasterplus.presentation.bluetooth_screen.Routes
import com.example.syncmasterplus.presentation.chat_screen.components.RowWithProgress
import com.example.syncmasterplus.presentation.device_screen.components.FabWithBottomSheet

@Composable
fun DeviceScreen(
    navController: NavController,
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onStartServer: () -> Unit,
    loadOldMessages: (device: BluetoothDevice) -> Unit
){
    if(state.isConnected){

        navController.navigate(
            route = Routes.CHAT_SCREEN,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        BluetoothDeviceList(
            onStartScan,
            onStopScan,
            pairedDevices = state.pairedDevices,
            scannedDevices = state.scannedDevices,
            onClick = {

                loadOldMessages(it)
                state.peerDevice = it
                navController.navigate(
                    route = Routes.CHAT_SCREEN,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FabWithBottomSheet(onStartServer)
        }

    }

}


@Composable
fun BluetoothDeviceList(
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    pairedDevices: List<BluetoothDeviceDomain>,
    scannedDevices: List<BluetoothDeviceDomain>,
    onClick: (BluetoothDeviceDomain) -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier
    ) {


        item {
            RowWithProgress(onStartScan,
                onStopScan,
                modifier = modifier)
        }

        if(scannedDevices.isEmpty()){
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Warning, contentDescription = "",
                        modifier = Modifier

                            .alpha(.6f),)
                    Text(
                        text = "No hay dispositivos",
                        modifier = Modifier
                            // .padding(16.dp)
                            .alpha(.6f),

                        )

                }

            }
        }else {
            items(scannedDevices) { device ->
                Text(
                    text = device.name ?: "(Sin nombre)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(device) }
                        .padding(16.dp)
                )

            }
        }

        item {
            Text(
                text = "Dispositivos Emparejados",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,

                modifier = Modifier.padding(16.dp)
            )
        }
        items(pairedDevices) { device ->
            Text(

                text = device.name ?: "(Sin nombre)",

                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp)
            )

        }
    }
}