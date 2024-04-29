package com.example.syncmasterplus.presentation.device_screen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.syncmasterplus.R

@Composable
fun FabWithBottomSheet(
    onStartServer: () -> Unit,
) {
    // booleano verifica si bottonsheet esta abierto
    val (isBottomSheetOpen, setIsBottomSheetOpen) = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ExtendedFloatingActionButton(
            onClick = {
                onStartServer()
                setIsBottomSheetOpen(!isBottomSheetOpen)
            },
            icon = { Icon(
                painterResource(id = R.drawable.bluetooth_searching)
                , "Action button.",
                tint = Color.White
                )
                   },
            text = { Text(text = "Iniciar servidor",
                color = Color.White,
                style = TextStyle(fontWeight = FontWeight.Bold)
                ) },
            modifier = Modifier.padding(10.dp),
            containerColor = colorResource(id = R.color.black90)
        )

        if (isBottomSheetOpen) {
            BottomSheet(
                onDismiss = { setIsBottomSheetOpen(false) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    onDismiss: () -> Unit
) {

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        dragHandle = { BottomSheetDefaults.DragHandle(
            color = colorResource(id = R.color.black90),
        ) },

    ) {

        Column (
            modifier = Modifier.fillMaxWidth()
                .fillMaxHeight(0.3f),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text(text = "Esperando conexión...")
            RippleLoadingAnimation()
        }
    }

}

@Preview
@Composable
fun PreviewComposableWithFabAndBottomSheet() {
    FabWithBottomSheet({})
}
