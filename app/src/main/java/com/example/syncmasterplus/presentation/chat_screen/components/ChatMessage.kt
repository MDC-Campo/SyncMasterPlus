package com.example.syncmasterplus.presentation.chat_screen.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import com.example.syncmasterplus.ui.theme.BlueViolet3
import com.example.syncmasterplus.ui.theme.GreyGreen
import com.example.syncmasterplus.ui.theme.SyncMasterPlusTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessage(
    message: BluetoothMessage.TextMessage,
    modifier: Modifier = Modifier,
    deleteMessage:(message: BluetoothMessage) -> Unit,

    ) {
    val context = LocalContext.current
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
            )
    ) {

        Text(
            text = message.text,
            color = Color.White,
            modifier = Modifier.widthIn(max = 250.dp)
        )
        Text(
            text = message.time,
            fontSize = 10.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Preview
@Composable
fun ChatMessagePreview() {
    SyncMasterPlusTheme {
        ChatMessage(
            message = BluetoothMessage.TextMessage(
                text = "Hello World!",
                senderName = "Pixel 6",
                senderAddress = "address",
                date = "11, Jan",
                time = "11:00 AM",
                isFromLocalUser = false
            ),
            deleteMessage = {},
            )
    }


}