package dev.danielc.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.danielc.R
import dev.danielc.common.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DisconnectDialog(nameOfDevice: String = "FooBar") {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(text = "Disconnect")
        },
        text = {
            Text(text = "Disconnect from ${nameOfDevice}?")
        },
        onDismissRequest = {

        },
        confirmButton = {
            TextButton(
                onClick = {

                }
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {

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

                }) {
                    Text("Grant")
                }
            }
        }
    }
}

class JobProgressBarModel : ViewModel() {
    private val _uiState = MutableStateFlow(0)
    val uiState: StateFlow<Int> = _uiState.asStateFlow()

    fun update(p: Int) {
        viewModelScope.launch() {
            withContext(Dispatchers.Default) {
                _uiState.value = p
            }
        }
    }
}