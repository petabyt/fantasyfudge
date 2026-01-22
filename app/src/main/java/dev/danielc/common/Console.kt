package dev.danielc.common

import android.content.ClipData
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.R
import kotlinx.coroutines.launch
import java.io.Console
import java.util.Locale

data class ConsoleState(
    val text: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewConsoleScreen(navController: NavHostController = rememberNavController(), state: ConsoleState = ConsoleState()) {
    var x: String = "";
    for (i in 0..100) {
        x += String.format(Locale.US, "Testing %d\n", i)
    }
    ConsoleScreen(state = ConsoleState(
        text = x
    ))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(navController: NavHostController = rememberNavController(), state: ConsoleState = ConsoleState()) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Test Suite")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigateUp()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                val clipData = ClipData.newPlainText("label", state.text)
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
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                Row {
                    val m = Modifier.weight(1f).padding(5.dp)
                    val color = colorResource(R.color.white)
                    Widgets.GreenButton(modifier = m, onClick = {}, content = {Text("Connect", color = color)})
                    Widgets.BlueButton(modifier = m, onClick = {}, content = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select WiFi", color = color)
                            Icon(
                                painter = painterResource(R.drawable.baseline_wifi_tethering_24),
                                tint = color,
                                contentDescription = "asd"
                            )
                        }
                    })
                }
                Column(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(5.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = state.text,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}