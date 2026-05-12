package dev.danielc.common.screens

import android.content.ClipData
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.danielc.R
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.errorButtonColors
import kotlinx.coroutines.launch

@Preview(showBackground = true, device = "id:pixel_9a", uiMode = 32)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectingScreen(back: () -> Unit = {}, tryAgain: () -> Unit = {}, state: ConsoleState = ConsoleState()) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
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
                Column(Modifier.padding(10.dp)) {
                    Button(onClick = tryAgain, Modifier.fillMaxWidth()) {
                        Text("Try again")
                    }
                    Button(onClick = back, Modifier.fillMaxWidth(), colors = errorButtonColors()) {
                        Text("Give up")
                    }
                }
                Console(Modifier.fillMaxSize(), state)
            }
        }
    }
}