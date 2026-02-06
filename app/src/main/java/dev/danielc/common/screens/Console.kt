package dev.danielc.common.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.R
import dev.danielc.common.Widgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.plus
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.toDuration

data class Line(
    val line: String,
    val timestamp: Duration? = null,
    val color: Color? = null,
)

data class ConsoleState(
    val initialLines: List<Line> = emptyList(),
    val initialTime: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow(),
    var title: String = "Test Suite",
    var lines: List<Line> = initialLines,
) {
    override fun toString(): String {
        var text = ""
        for (x in lines) {
            text += x.line + "\n"
        }
        return text
    }
}

class ConsoleStateModel(initialText: String? = null, initialLines: List<Line> = emptyList()) : ViewModel() {
    private val _uiState = MutableStateFlow(ConsoleState(initialLines))
    val uiState: StateFlow<ConsoleState> = _uiState.asStateFlow()

    fun addLine(line: String) {
        viewModelScope.launch() {
            withContext(Dispatchers.Default) {
                _uiState.update { currentState ->
                    val newLine = Line(
                        line = line,
                        timestamp = currentState.initialTime.elapsedNow()
                    )
                    currentState.copy(lines = currentState.lines + newLine)
                }
            }
        }
    }

    fun clearText(line: String) {
        viewModelScope.launch() {
            withContext(Dispatchers.Default) {
                _uiState.value.lines = emptyList()
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_9a", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewConsoleScreen(navController: NavHostController = rememberNavController()) {
    var x: List<Line> = emptyList()
    for (i in 0..10) {
        x += (Line(
            line = "Hello world",
            timestamp = i.toDuration(DurationUnit.SECONDS)
        ))
    }
    val model = ConsoleStateModel(initialLines = x)
    ConsoleScreen(model = model, buttons = {
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
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(navController: NavHostController = rememberNavController(), model: ConsoleStateModel = ConsoleStateModel(), buttons: @Composable () -> Unit = {}) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val state by model.uiState.collectAsStateWithLifecycle()
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
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                buttons()
                LazyColumn(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(5.dp)
                        //.verticalScroll(rememberScrollState())
                ) {
                    items(state.lines) { line ->
                        SelectionContainer {
                            Row {
                                val style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                                if (line.timestamp != null) {
                                    Text(
                                        text = "${line.timestamp.inWholeSeconds}",
                                        style = style,
                                        modifier = Modifier.width(20.dp)
                                    )
                                }
                                Text(
                                    text = line.line,
                                    style = style
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}