package dev.danielc.common.screens

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.danielc.R
import dev.danielc.common.ModuleManifest
import dev.danielc.common.ui.dummyManifestList
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.errorButtonColors
import kotlinx.coroutines.launch

data class UserInstruction(
    val instruction: String,
)

enum class ConnectingRequiredAction {
    NONE,
    TURN_ON_WIFI,
    TURN_ON_BLUETOOTH,
    ACCEPT_PERMISSION,
}

//@Preview(showBackground = true, device = "id:pixel_9a", uiMode = 32)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleErrorScreen(back: () -> Unit = {}, state: ConsoleState = ConsoleState()) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PakModule Failure") },
                    navigationIcon = {
                        IconButton(onClick = { back() }) {
                            Icon(painterResource(R.drawable.outline_arrow_back_24), contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("label", state.toString())
                                clipboardManager.setClipEntry(clipData.toClipEntry())
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_content_copy_24),
                                contentDescription = "Copy"
                            )
                        }
                    }
                )
            },
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Module failed to initialize. This is a bug.")
                    Console(Modifier.fillMaxSize(), state)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_9a", uiMode = 32)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectingScreen(back: () -> Unit = {}, tryAgain: () -> Unit = {}, state: ConsoleState = ConsoleState(), progress: Int? = 50,
                     action: ConnectingRequiredAction = ConnectingRequiredAction.TURN_ON_BLUETOOTH,
                     target: ModuleManifest.Target = dummyManifestList[0].targets[0],
                     transport: ModuleManifest.Transport = ModuleManifest.Transport.WIFI_AP) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    @Composable
    fun ActionMessage(icon: Painter, text: String) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(200.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text, textAlign = TextAlign.Center)
            // TODO: Disable button during connecting
            Button({
                tryAgain()
            }) {
                Text("Try Again")
            }
        }
    }

    BackHandler {
        back()
    }

    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Connecting")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            back()
                        }) {
                            Icon(painterResource(R.drawable.outline_arrow_back_24), contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("label", state.toString())
                                clipboardManager.setClipEntry(clipData.toClipEntry())
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_content_copy_24),
                                contentDescription = "Copy"
                            )
                        }
                    }
                )
            },
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                if (action == ConnectingRequiredAction.TURN_ON_WIFI) {
                    ActionMessage(painterResource(R.drawable.outline_wifi_24), "Please turn on WiFi")
                } else if (action == ConnectingRequiredAction.TURN_ON_BLUETOOTH) {
                    ActionMessage(painterResource(R.drawable.outline_bluetooth_24), "Please turn on Bluetooth")
                } else if (action == ConnectingRequiredAction.ACCEPT_PERMISSION) {
                    ActionMessage(painterResource(R.drawable.outline_bluetooth_24), "Permission required to connect to a device")
                } else {
                    Column(Modifier.padding(10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp)) {
                            Icon(painterResource(target.deviceId.getIcon()), contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (transport == ModuleManifest.Transport.LOCAL_NETWORK_UDP) {
                                    Text(
                                        "Looking for a ${target.company} ${target.deviceId.getReadableName()}...",
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        "Connecting to a ${target.company} ${target.deviceId.getReadableName()}...",
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (progress != null) Text("${progress}%", textAlign = TextAlign.Center)
                            }
                        }
                        Button(onClick = tryAgain, Modifier.fillMaxWidth()) {
                            Text("Try again")
                        }
                        Button(onClick = back, Modifier.fillMaxWidth(), colors = errorButtonColors()) {
                            Text("Cancel")
                        }
                        if (transport == ModuleManifest.Transport.LOCAL_NETWORK_UDP) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            if (progress != null ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    progress = { progress.toFloat() / 100 }
                                )
                            }
                        }
                    }
                    Console(Modifier.fillMaxSize(), state)
                }
            }
        }
    }
}