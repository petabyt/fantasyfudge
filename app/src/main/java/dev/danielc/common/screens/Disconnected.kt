package dev.danielc.common.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.Typography

@Composable
fun PreviewConsole() {
    val state = ConsoleState(initialLines = listOf())
    Console(state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun DisconnectedScreen(reason: String = "Reason: XXX", backToMainScreen: () -> Unit = {}, info: @Composable () -> Unit = { PreviewConsole() }) {
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
                    Text("Disconnected", style = MaterialTheme.typography.titleLarge)
                    Text(reason, style = MaterialTheme.typography.labelMedium)
                    Button(onClick = {
                        backToMainScreen()
                    }) {
                        Text("Exit")
                    }
                    info()
                }
            }
        }
    }
}