package dev.danielc.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DisconnectDialog(nameOfDevice: String = "FooBar", yes: () -> Unit = {}, no: () -> Unit = {}) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(text = "Disconnect")
        },
        text = {
            Text(text = "Disconnect from ${nameOfDevice}?")
        },
        onDismissRequest = {
            no()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    yes()
                }
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    no()
                }
            ) {
                Text("No")
            }
        }
    )
}

@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PermissionDialog(proceed: () -> Unit = {}, reject: () -> Unit = {}) {
    Dialog(onDismissRequest = {
        reject()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                Text(
                    text = "Android permission needed",
                    modifier = Modifier,
                    style = TextStyle(
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Required to connect to bluetooth devices",
                    modifier = Modifier,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = {
                    proceed()
                }) {
                    Text("Grant")
                }
            }
        }
    }
}