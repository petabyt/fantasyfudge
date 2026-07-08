package dev.danielc.common.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.Command
import dev.danielc.common.ModuleInstance
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.errorIconButtonColors
import dev.danielc.common.ui.theme.primaryIconButtonColors
import dev.danielc.common.ui.theme.secondaryIconButtonColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class IntervalometerModel(val module: ModuleInstance): ViewModel() {
    var job: Job? = null
    val status = MutableStateFlow("Not started")
    val doingCapture = MutableStateFlow(false)
    fun stop() {
        job?.cancel()
        job = null
        status.value = "Stopping"
    }
    override fun onCleared() {
        super.onCleared()
        stop()
    }
    fun start(num: Int, interval: Int) {
        if (doingCapture.value) return
        job = CoroutineScope(Dispatchers.IO).launch {
            doingCapture.value = true
            for (i in 0..num) {
                if (!isActive) break
                status.value = "Capturing #${i + 1}"
                module.runCommand(Command.PAK_CMD_SHUTTER_DOWN)
                module.runCommand(Command.PAK_CMD_SHUTTER_UP)
                if (!isActive) break
                try {
                    delay((1000 * interval).toLong())
                } catch (e: CancellationException) {
                    break
                }
            }
            status.value = "Stopped"
            doingCapture.value = false
        }
    }
    fun shutter(press: Boolean) {
        if (doingCapture.value) return
        job = CoroutineScope(Dispatchers.IO).launch {
            module.runCommand(if (press) Command.PAK_CMD_SHUTTER_DOWN else Command.PAK_CMD_SHUTTER_UP)
        }
    }
}

@Composable
fun Intervalometer(modifier: Modifier = Modifier, model: IntervalometerModel) {
    val doingCapture by model.doingCapture.collectAsStateWithLifecycle()
    val status by model.status.collectAsStateWithLifecycle()
    Intervalometer(modifier, start = { n, s ->
        model.start(n, s)
    }, stop = {
        model.stop()
    }, shutter = { press ->
        model.shutter(press)
    }, doingCapture = doingCapture, status = status)
}

@Composable
fun Intervalometer(modifier: Modifier = Modifier, start: (Int, Int) -> Unit = {a, b ->}, stop: () -> Unit = {}, shutter: (Boolean) -> Unit = {}, doingCapture: Boolean = false, status: String = "") {
    val haptic = LocalHapticFeedback.current
    Column(modifier.padding(10.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        var shotsToTake by remember { mutableStateOf("10") }
        var secondsInBetweenShots by remember { mutableStateOf("1") }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(
                leadingIcon = {
                    Icon(painterResource(R.drawable.outline_numbers_24), contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                value = shotsToTake,
                onValueChange = { shotsToTake = it },
                label = { Text("How many shots to take") }
            )
            TextField(
                leadingIcon = {
                    Icon(painterResource(R.drawable.outline_watch_later_24), contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                value = secondsInBetweenShots,
                onValueChange = { secondsInBetweenShots = it },
                label = { Text("Seconds inbetween each shot") }
            )

            Text("Status: ${status}")

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (doingCapture) {
                    IconButton(
                        modifier = Modifier.size(100.dp),
                        colors = errorIconButtonColors(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            stop()
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.baseline_stop_24), contentDescription = null, modifier = Modifier.size(50.dp)
                        )
                    }
                } else {
                    IconButton(
                        modifier = Modifier.size(100.dp),
                        colors = secondaryIconButtonColors(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            start(shotsToTake.toInt(), secondsInBetweenShots.toInt())
                        }
                    ) {
                        Icon(painterResource(R.drawable.outline_shutter_speed_24), contentDescription = null, modifier = Modifier.size(50.dp))
                    }
                }
            }
        }

        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Press) {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        shutter(true)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        shutter(false)
                    }
                }
            }
            IconButton(
                interactionSource = interactionSource,
                modifier = Modifier.size(200.dp),
                colors = if (isPressed) primaryIconButtonColors(0.8f) else primaryIconButtonColors(),
                enabled = true,
                onClick = {  },
            ) {
                Icon(painterResource(R.drawable.outline_camera_24), contentDescription = null, modifier = Modifier.size(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun IntervalometerScreen(navController: NavHostController = rememberNavController()) {
    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("Shutter")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigateUp()
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
            Column(Modifier.padding(innerPadding)) {
                Intervalometer()
            }
        }
    }
}