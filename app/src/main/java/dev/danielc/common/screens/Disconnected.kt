package dev.danielc.common.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.danielc.R
import dev.danielc.common.ui.theme.FudgeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun DisconnectedScreen(reason: String = "Reason: Failed to connect - (Disconnected)", backToMainScreen: () -> Unit = {}, consoleState: ConsoleState = ConsoleState()) {
    return FudgeTheme {
        BackHandler {
            backToMainScreen()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("Disconnected")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            backToMainScreen()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.outline_arrow_back_24),
                                contentDescription = null
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
//                    Text(reason, style = MaterialTheme.typography.labelMedium)
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        backToMainScreen()
                    }) {
                        Text("Exit")
                    }
                    Console(Modifier.weight(1f), consoleState)
                }
            }
        }
    }
}