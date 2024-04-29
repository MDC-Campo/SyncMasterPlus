package com.example.syncmasterplus.presentation.chat_screen.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncmasterplus.R
import com.example.syncmasterplus.domain.chat.bluetooth.entity.BluetoothMessage
import com.example.syncmasterplus.ui.theme.BlueViolet3
import com.example.syncmasterplus.ui.theme.GreyGreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageMessage(
    message: BluetoothMessage.ImageMessage,
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
                        .makeText(context, "Mensaje eliminado", Toast.LENGTH_SHORT)
                        .show()
                }
            )
    ) {

        val imageBitmap = byteArrayToPainter(message.imgData)
        if(imageBitmap!=null){
            Image(bitmap =imageBitmap , contentDescription = "",
                modifier = Modifier.size(250.dp))
        }else{
            Image(
                painterResource(id = R.drawable.place_holder) , contentDescription = "",
                modifier = Modifier.size(250.dp))
        }


        Text(
            text = message.time,
            fontSize = 10.sp,
            color = Color.Black,
            modifier = Modifier.align(Alignment.End)
        )
    }
}
fun byteArrayToPainter(byteArray: ByteArray): ImageBitmap? {
    return try {
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        val imageBitmap = bitmap.asImageBitmap()
        imageBitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}