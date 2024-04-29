package com.example.syncmasterplus.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PermissionDialog(
    onOkClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    AlertDialog(
        onDismissRequest = onDismissClick,
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Permitir permiso",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOkClick()
                            onDismissClick()
                        }
                        .padding(16.dp)
                )
            }
        },
        title = {
            Text(text = "Permiso requerido")
        },
        text = {
            Text(text = "Para dispositivos con android versión < 11.0" + "requiere permiso de ubicación")
        },
        modifier = modifier
    )
}

@Preview
@Composable
fun PermissionDialogPreview(){
    PermissionDialog(
        onOkClick = {},
        onDismissClick = {}
    )
}