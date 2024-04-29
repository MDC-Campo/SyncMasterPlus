package com.example.syncmasterplus.presentation.chat_screen

import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncmasterplus.R
import com.example.syncmasterplus.domain.chat.bluetooth.BluetoothDeviceDomain
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import com.example.syncmasterplus.domain.chat.bluetooth.entity.toMessageEntity
import com.example.syncmasterplus.domain.chat.image.UriToByteArray
import com.example.syncmasterplus.domain.chat.util.DateAndTime
import com.example.syncmasterplus.presentation.BluetoothUiState
import com.example.syncmasterplus.presentation.chat_screen.components.AudioMessage
import com.example.syncmasterplus.presentation.chat_screen.components.ChatMessage
import com.example.syncmasterplus.presentation.chat_screen.components.ImageMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(

    navController: NavController,
    state: BluetoothUiState,
    sendTextMessage: (String) -> Unit,
    sendAudioMessage: () -> Unit,
    sendImageMessage: (ByteArray) -> Unit,
    connectToDevice: (BluetoothDeviceDomain) -> Unit,
    disconnectFromDevice: () -> Unit,
    startRecording: () -> Unit,

    stopRecording: () -> Unit,
    createAudioFile: (name: String) -> File,
    startPlaying: () -> Unit,
    stopPlaying: () -> Unit,
    seekTo: (position: Int) -> Unit,
    getAudioDuration: () -> Int,
    getCurrentPosition: () -> Int,
    saveByteArrayToFile: (ByteArray, File) -> Unit,
    setPlayer: (File) -> Unit,
    deleteMessage: (message: BluetoothMessage) -> Unit,
    exportChat: (messages: List<BluetoothMessage>) -> String

) {

    var isRecording by remember { mutableStateOf(false) }
    val message = rememberSaveable {
        mutableStateOf("")
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
            it?.let { uri ->
                selectedImageUri = uri
            }
        }

    val uriToByteArray = UriToByteArray()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = state.peerDevice?.name ?: "Sin nombre",
                modifier = Modifier.weight(1f)
            )


            if (state.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )

            } else if (state.isConnected) {
                IconButton(onClick = {
                    disconnectFromDevice()
                }

                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.exit),
                        contentDescription = "Disconnect"
                    )
                }
            } else {
                Button(onClick = { state.peerDevice?.let { connectToDevice(it) } }) {
                    Text(text = "Conectar")
                }
            }

        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            reverseLayout = true
        ) {

            items(state.messages) { message ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val date = message.toMessageEntity("").date
                    val nextIndex = state.messages.indexOf(message) + 1
                    if (nextIndex >= state.messages.size) {
                        Text(
                            text = if (date == DateAndTime.getTodayDate()) {
                                "Today"
                            } else if (date == DateAndTime.getPreviousDate(DateAndTime.getTodayDate())) {
                                "Yesterday"
                            } else date,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else if (date != state.messages[nextIndex].toMessageEntity("").date) {
                        Text(
                            text = if (date == DateAndTime.getTodayDate()) {
                                "Today"
                            } else if (date == DateAndTime.getPreviousDate(DateAndTime.getTodayDate())) {
                                "Yesterday"
                            } else date,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    when (message) {
                        is BluetoothMessage.TextMessage -> {

                            ChatMessage(
                                message = message,
                                modifier = Modifier
                                    .align(
                                        if (message.isFromLocalUser) Alignment.End else Alignment.Start
                                    ),
                                deleteMessage = deleteMessage,

                                )
                        }

                        is BluetoothMessage.AudioMessage -> {

                            AudioMessage(
                                message = message,
                                modifier = Modifier
                                    .align(
                                        if (!message.isFromLocalUser) Alignment.Start else Alignment.End
                                    ),

                                startPlaying,
                                stopPlaying,
                                seekTo,
                                getAudioDuration,
                                getCurrentPosition,
                                createAudioFile,
                                saveByteArrayToFile,
                                setPlayer,
                                deleteMessage
                            )
                        }

                        is BluetoothMessage.ImageMessage -> {

                            ImageMessage(
                                message = message,
                                modifier = Modifier
                                    .align(
                                        if (!message.isFromLocalUser) Alignment.Start else Alignment.End
                                    ),
                                deleteMessage

                            )
                        }
                    }

                }
            }
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {

            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text("Grabando...") },
                    leadingIcon = {
                        Icon(
                            painterResource(
                                id = R.drawable.mic_24
                            ), contentDescription = ""
                        )
                    },

                    )
            }

        }
        //image selection

        selectedImageUri?.let { uri ->

            Log.e("IMAGE", "ChatScreen: ${uri.toString()}")

            val imageData = uriToByteArray.uriToByteArray(
                contentResolver = context.contentResolver,
                uri = uri
            )
            selectedImageUri = null
            Toast.makeText(context, "Enviando imagen por favor espere...", Toast.LENGTH_SHORT).show()
            sendImageMessage(imageData!!)


        }



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {


            TextField(
                value = message.value,
                onValueChange = { message.value = it },
                modifier = Modifier
                    .heightIn(0.dp, 200.dp)
                    .weight(1f),
                placeholder = {
                    Text(text = "Nuevo mensaje")
                },
                leadingIcon = {

                    IconButton(
                        onClick = {

                            launcher.launch(arrayOf("image/*"))
                        },
                    ) {
                        Icon(
                            painterResource(id = R.drawable.insert_photo),
                            contentDescription = "Img"
                        )
                    }
                }
            )
            ButtonWithRecording(
                isRecording,
                onRecordingChanged = { isRecording = it },
                sendAudioMessage = sendAudioMessage,
                startRecording,
                stopRecording,
                createAudioFile,
            )
            IconButton(
                onClick = {

                    sendTextMessage(message.value)
                    message.value = ""
                    keyboardController?.hide()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
    BackHandler {
        disconnectFromDevice()
        navController.navigateUp()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ButtonWithRecording(
    isRecording: Boolean,
    onRecordingChanged: (Boolean) -> Unit,
    sendAudioMessage: () -> Unit,
    startRecording: () -> Unit,
    stopRecording: () -> Unit,
    createAudioFile: (name: String) -> File,
) {


    val transition = updateTransition(targetState = isRecording, label = "")
    val coroutineScope = rememberCoroutineScope()

    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 300)
            } else {
                tween(durationMillis = 300)
            }
        }, label = ""
    ) { recordingState ->
        if (recordingState) 2f else 1f
    }


    IconButton(
        onClick = {},
        modifier = Modifier
            .padding(8.dp)

            .pointerInteropFilter { event ->
                when {
                    event.action == MotionEvent.ACTION_DOWN -> {
                        onRecordingChanged(true)
                        coroutineScope.launch(Dispatchers.IO) {
                            createAudioFile("new_recording")
                            delay(100)

                            startRecording()
                        }
                    }

                    event.action == MotionEvent.ACTION_UP -> {
                        onRecordingChanged(false)
                        stopRecording()
                        sendAudioMessage()
                    }
                }
                true
            }
            .scale(scale)

    ) {
        Icon(painterResource(id = R.drawable.mic_24), contentDescription = "")
    }
}