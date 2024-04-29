package com.example.syncmasterplus.presentation.bluetooth_screen

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.syncmasterplus.presentation.chat_screen.ChatScreen
import com.example.syncmasterplus.presentation.device_screen.DeviceScreen
import com.example.syncmasterplus.presentation.view_model.BluetoothViewModel

@Composable
fun BluetoothScreen(context: Context){
    val navController = rememberNavController()
    val viewModel = hiltViewModel<BluetoothViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(key1 = state.errorMessage) {
        state.errorMessage?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(key1 = state.isConnected) {
        if(state.isConnected) {
            Toast.makeText(
                context,
                "Conectado!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    NavHost(navController =navController
        , startDestination = Routes.DEVICE_SCREEN
    ) {
        composable(
            Routes.CHAT_SCREEN
        ) {
            ChatScreen(
                navController, state,
                sendTextMessage = viewModel::sendMessage,
                sendAudioMessage = viewModel::sendMessage,
                sendImageMessage = viewModel::sendMessage,
                viewModel::connectToDevice,
                viewModel::disconnectFromDevice,
                startRecording = viewModel::startRecord,
                stopRecording = viewModel::stopRecord,
                createAudioFile = viewModel::createAudioFile,
                startPlaying = viewModel::startPlay,
                stopPlaying = viewModel::stopPlay,
                seekTo = viewModel::seekTo,
                getAudioDuration = viewModel::getAudioDuration,
                getCurrentPosition = viewModel::getCurrentPosition,
                saveByteArrayToFile = viewModel::saveByteArrayToFile,
                setPlayer = viewModel::setPlayer,
                deleteMessage = viewModel::deleteMessage,
                exportChat = viewModel::exportChatToTextFile

            )
        }
        composable(Routes.DEVICE_SCREEN) {
            DeviceScreen(
                navController,
                state,
                viewModel::startScan,
                viewModel::stopScan,

                viewModel::waitForIncomingConnections,
                loadOldMessages = viewModel::loadOldMessages
            )
        }
        composable(Routes.SPLASH_SCREEN) {
        }
    }
}

object Routes {
    const val CHAT_SCREEN = "chatScreen"
    const val SPLASH_SCREEN = "splashScreen"
    const val DEVICE_SCREEN = "deviceScreen"
}