package com.example.syncmasterplus.presentation.chat_screen.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncmasterplus.R
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import com.example.syncmasterplus.domain.chat.playback.AndroidAudioPlayer
import com.example.syncmasterplus.ui.theme.BlueViolet3
import com.example.syncmasterplus.ui.theme.GreyGreen
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioMessage(
    message: BluetoothMessage.AudioMessage,
    modifier: Modifier = Modifier,
    startPlaying: () -> Unit,
    stopPlaying: () -> Unit,
    seekTo: (position: Int) -> Unit,
    getAudioDuration:() ->Int,
    getCurrentPosition:() ->Int,
    createAudioFile:(name:String)-> File,
    saveByteArrayToFile:(ByteArray, File)->Unit,
    setPlayer: (File)->Unit,
    deleteMessage:(message: BluetoothMessage) -> Unit,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(10f) }
    var audioFile by rememberSaveable { mutableStateOf<File?>(null) }

    val context = LocalContext.current

    var audioPlayer by remember {
        mutableStateOf(AndroidAudioPlayer(context))
    }

    if(audioFile==null){

        audioFile =  createAudioFile(UUID.randomUUID().toString())
        saveByteArrayToFile(message.audioData,audioFile!!)
        audioPlayer.playFile(audioFile!!)
        duration = audioPlayer.getAudioDuration().toFloat()
    }

    LaunchedEffect(isPlaying) {

        while (isPlaying) {
            delay(500L)
            progress = (progress + 500f ).coerceAtMost(duration)
            if(progress==duration){
                isPlaying=!isPlaying
                progress = 0f
            }
        }
    }

    Column(

        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = if (message.isFromLocalUser) 15.dp else 0.dp,
                    topEnd = 15.dp,
                    bottomStart = 15.dp,
                    bottomEnd = if (message.isFromLocalUser) 0.dp else 15.dp
                )
            )
            .background(
                if (message.isFromLocalUser) BlueViolet3 else GreyGreen
            )
            .padding(16.dp)
            .combinedClickable(
                onClick = {},
                onDoubleClick = {},
                onLongClick = {
                    deleteMessage(message)
                    Toast
                        .makeText(context, "Mensaje Eliminado", Toast.LENGTH_SHORT)
                        .show()
                }
            ),
    ) {


        Row(

            verticalAlignment = Alignment.CenterVertically

        ) {
            // Play button
            IconButton(
                onClick = {
                    if (isPlaying) {
                        audioPlayer.stop()
                        isPlaying = !isPlaying

                    } else {
                        if(message.audioData.isEmpty()){
                            Toast.makeText(context, "No se encontraron datos de audio.", Toast.LENGTH_SHORT).show()
                        }else{

                            audioPlayer.start()
                            isPlaying = !isPlaying
                        }
                    }
                },

                content = {
                    Icon(

                        painterResource(
                            id =
                            if (isPlaying) R.drawable.pause_24 else R.drawable.play_arrow_24
                        ),
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp),
                    )
                }
            )
            val durationInSeconds = duration / 1000.0
            val formattedDuration = String.format("%.1fs", durationInSeconds)
            Text(
                text = if(progress == 0f) formattedDuration else String.format("%.1fs", (progress/1000)),
                fontSize = 10.sp,
                color = Color.Black,
                modifier = Modifier.padding(8.dp)
            )
            Slider(
                value = progress,
                onValueChange = {
                    progress = it
                    audioPlayer.seekTo(it.toInt())

                },
                valueRange = 0f..duration,
                onValueChangeFinished = {
                },
                modifier = Modifier.fillMaxWidth(0.5f)
            )
            Text(
                text = message.time,
                fontSize = 10.sp,
                color = Color.Black,
            )
        }

    }
}


