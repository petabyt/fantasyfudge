package dev.danielc.common.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.DashboardPane
import dev.danielc.common.Device
import dev.danielc.common.ModuleManifest
import dev.danielc.common.ui.IntGridGraph
import dev.danielc.common.ui.Material3DropDown
import dev.danielc.common.ui.PreviewPixel9ProDark
import dev.danielc.common.ui.theme.FudgeRippleConfig
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DashboardState(
    val manifest: ModuleManifest?,
    val panes: List<DashboardPane> = emptyList(),
    val batteryLevelMain: Int? = null,
    val batteryLevelLeft: Int? = null,
    val batteryLevelRight: Int? = null,
    val nameOfDevice: String? = null,
    val filesOnStorage: Int? = null,
    val temperature: Int? = null,
    val humidity: Int? = null,
    val firmwareVersion: String? = null,
    val connectionType: ModuleManifest.Transport? = null,
)

data class DashboardCallbacks(
    val updatePaneValue: (DashboardPane) -> Unit = { },
    val disconnect: () -> Unit = { },
    val runCommand: (String) -> Unit = { },
)

data class PaneState(
    val color: PaneState.Color = PaneState.Color.SECONDARY,
    val text: String? = null,
    val icon: Int? = null,
    val onClick: () -> Unit = {},
    val content: (@Composable () -> Unit)? = null,
    val fillMaxWidth: Boolean = false,
) {
    enum class Color {
        PRIMARY,
        SECONDARY,
        TERTIARY,
        ERROR,
        NEUTRAL,
    }
}

fun cameraState(): DashboardState {
    val manifest = ModuleManifest(name = "Fujifilm", targets = listOf(ModuleManifest.Target(deviceId = Device.PROFESSIONAL_CAMERA)))
    return DashboardState(
        manifest = manifest,
        nameOfDevice = "Fujifilm X100VI",
        filesOnStorage = 321,
        batteryLevelMain = 78,
        firmwareVersion = "0.1.0",
    )
}

//@PreviewPixel9ProDark
@Composable
fun PreviewDashboardCamera() {
    var state by remember { mutableStateOf(cameraState()) }
    return FudgeTheme {
        Scaffold { innerPadding ->
            Dashboard(Modifier.padding(innerPadding), state = state, callbacks = DashboardCallbacks())
        }
    }
}

fun budsState(): DashboardState {
    val manifest = ModuleManifest(name = "CMF Nothing", description = "Supports", targets = listOf(ModuleManifest.Target(deviceId = Device.EARBUDS)))
    val state = DashboardState(
        manifest = manifest,
        nameOfDevice = "CMF Buds Pro 2",
        firmwareVersion = "5.0",
        panes = listOf(
            DashboardPane.BooleanSetting(
                DashboardPane.Properties("nc", "Noise cancellation"),
                value = true
            ),
            DashboardPane.BooleanSetting(
                DashboardPane.Properties("be", "Bass enhancement"),
                value = false
            ),
            DashboardPane.IntSetting(
               DashboardPane.Properties("st", "Something"),
                value = 123
            ),
            DashboardPane.DropdownSetting(
                DashboardPane.Properties("temp", "Dropdown"),
                index = 2,
                options = listOf("4.0l I6", "5.6l v8", "7.4l v8", "2.8l tdi")
            ),
            DashboardPane.Graph(
                DashboardPane.Properties("temp", "Graph"),
                points = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 8, 7, 6, 5, 4, 5, 7, 8, 5)
            ),
        )
    )
    return state
}

@PreviewPixel9ProDark
@Composable
fun PreviewDashboardBuds() {
    var state by remember { mutableStateOf(budsState()) }
    return FudgeTheme {
        Scaffold(
            content = { innerPadding ->
                Dashboard(Modifier.padding(innerPadding), state = state, callbacks = DashboardCallbacks())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardPane(modifier: Modifier = Modifier, bg: Color, fg: Color, content: @Composable () -> Unit, onClick: () -> Unit) {
    CompositionLocalProvider(LocalRippleConfiguration provides FudgeRippleConfig(fg)) {
        Box(modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .indication(
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() }
            )
        ) {
            content()
        }
    }
}

fun getBatteryStatusIcon(percent: Int): Int {
    return when (percent) {
        0 -> R.drawable.outline_battery_android_0_24
        in 1..13 -> R.drawable.outline_battery_android_frame_1_24
        in 14..44 -> R.drawable.outline_battery_android_frame_2_24
        in 45..58 -> R.drawable.outline_battery_android_frame_3_24
        in 59..72 -> R.drawable.outline_battery_android_frame_4_24
        in 73..86 -> R.drawable.outline_battery_android_frame_5_24
        in 87..99 -> R.drawable.outline_battery_android_frame_6_24
        100 -> R.drawable.outline_battery_android_frame_full_24
        else -> R.drawable.outline_battery_android_0_24
    }
}

data class PaneBatteryStatus(
    val name: String,
    val percent: Int,
)

fun BatteryListPane(batteries: List<PaneBatteryStatus>): PaneState {
    return PaneState(PaneState.Color.NEUTRAL, content = {
        Text("Battery Status", color = MaterialTheme.colorScheme.onSurface)
        Row(Modifier.fillMaxWidth()) {
            for (b in batteries) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(getBatteryStatusIcon(b.percent)), contentDescription = null)
                    Text("${b.percent}%", style = MaterialTheme.typography.labelSmall)
                    Text(b.name, style = MaterialTheme.typography.labelSmall, fontStyle = FontStyle.Italic)
                }
            }
        }
    })
}

@Composable
fun SettingsDialog(dashboardCallbacks: DashboardCallbacks, close: () -> Unit = {}) {
    Dialog(onDismissRequest = {
        close()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                var terminalCommand by remember { mutableStateOf("help") }
                TextField(
                    leadingIcon = {
                        Icon(painterResource(R.drawable.baseline_terminal_24), contentDescription = null)
                    },
                    value = terminalCommand,
                    onValueChange = { terminalCommand = it },
                    label = { Text("Terminal command") }
                )
                Button(onClick = {dashboardCallbacks.runCommand(terminalCommand)}) {
                    Text("Execute")
                }
            }
        }
    }
}

@Composable
fun DropdownDialog(close: () -> Unit = {}, dropdownSetting: DashboardPane.DropdownSetting, onSelect: (Int) -> Unit = {}) {
    Dialog(onDismissRequest = {
        close()
    }) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f)) {
            Text(dropdownSetting.args.title, modifier = Modifier.padding(10.dp))
            LazyColumn {
                itemsIndexed(dropdownSetting.options) { i, item ->
                    Box(Modifier.fillMaxWidth().clickable(onClick = {
                        onSelect(i)
                    }).background(if (i == dropdownSetting.index) MaterialTheme.colorScheme.surfaceContainer.copy(0.5f) else MaterialTheme.colorScheme.surfaceContainer)) {
                        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(item)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController(), state: DashboardState, callbacks: DashboardCallbacks) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedDropdown by remember { mutableStateOf<DashboardPane.DropdownSetting?>(null) }
    val coroutineScope = rememberCoroutineScope()
    if (showSettings) {
        SettingsDialog(callbacks, close = {
            showSettings = false
        })
    }
    selectedDropdown?.let { setting ->
        DropdownDialog({
            selectedDropdown = null
        }, setting, { i ->
            callbacks.updatePaneValue(setting.copy(index = i))
            coroutineScope.launch {
                delay(200)
                selectedDropdown = null
            }
        })
    }
    Column(
        modifier = modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.nameOfDevice != null) {
                        if (state.manifest != null && !state.manifest.targets.isEmpty())
                            Icon(
                                painter = painterResource(state.manifest.targets[0].deviceId.getIcon()),
                                contentDescription = null
                            )
                        Text(
                            state.nameOfDevice,
                            fontSize = 25.sp,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.End)) {
                        if (state.batteryLevelMain != null) {
                            Icon(
                                painter = painterResource(getBatteryStatusIcon(state.batteryLevelMain)),
                                contentDescription = null,
                            )
                        }
                        val icon = when (state.connectionType) {
                            ModuleManifest.Transport.BLUETOOTH -> R.drawable.outline_bluetooth_24
                            ModuleManifest.Transport.USB -> R.drawable.baseline_usb_24
                            else -> R.drawable.outline_wifi_24
                        }
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                        )
                    }
                }
                if (state.filesOnStorage != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(painter = painterResource(R.drawable.outline_photo_library_24), contentDescription = null)
                        Text("${state.filesOnStorage} files")
                    }
                }
                if (state.firmwareVersion != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(painter = painterResource(R.drawable.outline_developer_board_24), contentDescription = null)
                        Text("Firmware version: ${state.firmwareVersion}")
                    }
                }
            }
        }

        val panes = mutableListOf(
            PaneState(PaneState.Color.SECONDARY, "Settings", R.drawable.baseline_settings_24, onClick = {
                showSettings = true
            }),
            PaneState(PaneState.Color.ERROR, "Disconnect", R.drawable.outline_close_24, onClick = {
                callbacks.disconnect()
            }),
        )

        val batteries = mutableListOf<PaneBatteryStatus>()
        if (state.batteryLevelLeft != null) batteries.add(PaneBatteryStatus("Left", state.batteryLevelLeft))
        if (state.batteryLevelMain != null) batteries.add(PaneBatteryStatus("Base", state.batteryLevelMain))
        if (state.batteryLevelRight != null) batteries.add(PaneBatteryStatus("Right", state.batteryLevelRight))
        if (batteries.size > 1) panes += BatteryListPane(batteries)

        if (state.temperature != null) {
            panes += PaneState(PaneState.Color.NEUTRAL, content = {
                Row(Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.outline_device_thermostat_24), contentDescription = null)
                    Text("Temperature", color = MaterialTheme.colorScheme.onSurface)
                }
                val c = state.temperature.toFloat() / 100
                Text("%.2f C / %.2f F".format(c, c * 1.8 + 32))
            })
        }

        if (state.humidity != null) {
            panes += PaneState(PaneState.Color.NEUTRAL, content = {
                Row(Modifier.fillMaxWidth()) {
                    Icon(painterResource(R.drawable.outline_humidity_percentage_24), contentDescription = null)
                    Text("Humidity", color = MaterialTheme.colorScheme.onSurface)
                }
                Text("%.2f%%".format(state.humidity.toFloat() / 100))
            })
        }

        for (pane in state.panes) {
            panes += when (pane) {
                is DashboardPane.BooleanSetting -> {
                    PaneState(PaneState.Color.NEUTRAL, content = {
                        Text(pane.args.title, style = MaterialTheme.typography.titleSmall)
                        Switch(pane.value,
                            onCheckedChange = {
                                callbacks.updatePaneValue(pane.copy(value = !pane.value))
                            }
                        )
                    })
                }
                is DashboardPane.Button -> {
                    PaneState(PaneState.Color.PRIMARY, text = pane.args.title, onClick = {
                        callbacks.updatePaneValue(pane)
                    })
                }
                is DashboardPane.Graph -> {
                    PaneState(PaneState.Color.NEUTRAL, content = {
                        val coords = mutableListOf<Pair<Int, Int>>()
                        for (i in pane.points.indices) coords += Pair(i, pane.points[i])
                        Text(pane.args.title)
                        Box(Modifier.aspectRatio(1f)) {
                            IntGridGraph(coords, Modifier)
                        }
                    }, fillMaxWidth = true)
                }
                is DashboardPane.DropdownSetting -> {
                    PaneState(PaneState.Color.NEUTRAL, content = {
                        Text(pane.args.title, style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.background(MaterialTheme.colorScheme.surface).padding(10.dp).fillMaxWidth()) {
                            Text(pane.options[pane.index])
                            Spacer(Modifier.weight(1f))
                            Icon(painterResource(R.drawable.outline_arrow_forward_24), contentDescription = null)
                        }
                    }, onClick = {
                        selectedDropdown = pane
                    })
                }
                is DashboardPane.IntSetting -> {
                    PaneState(PaneState.Color.NEUTRAL, content = {
                        TextField(
                            leadingIcon = {
                                Icon(painterResource(R.drawable.outline_numbers_24), contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            value = pane.value.toString(),
                            onValueChange = { callbacks.updatePaneValue(pane.copy(value = it.toInt())) },
                            label = { Text(pane.args.title) }
                        )
                    })
                }
                is DashboardPane.SliderSetting -> {
                    PaneState()
                }
            }
        }

        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize(),
            columns = StaggeredGridCells.Adaptive(160.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                items(panes, span = {
                    if (it.fillMaxWidth) {
                        StaggeredGridItemSpan.FullLine
                    } else {
                        StaggeredGridItemSpan.SingleLane
                    }
                }) { pane ->
                    var bg: Color
                    var fg: Color
                    when (pane.color) {
                        PaneState.Color.PRIMARY -> {
                            bg = MaterialTheme.colorScheme.primary
                            fg = MaterialTheme.colorScheme.onPrimary
                        }
                        PaneState.Color.SECONDARY -> {
                            bg = MaterialTheme.colorScheme.secondary
                            fg = MaterialTheme.colorScheme.onSecondary
                        }
                        PaneState.Color.ERROR -> {
                            bg = MaterialTheme.colorScheme.error
                            fg = MaterialTheme.colorScheme.onError
                        }
                        PaneState.Color.NEUTRAL -> {
                            bg = MaterialTheme.colorScheme.surfaceContainer
                            fg = MaterialTheme.colorScheme.onSurface
                        }
                        PaneState.Color.TERTIARY -> {
                            bg = MaterialTheme.colorScheme.tertiary
                            fg = MaterialTheme.colorScheme.onTertiary
                        }
                    }
                    DashboardPane(
                        Modifier.fillMaxSize().wrapContentHeight(), // ?? not expanding pane size
                        bg = bg,
                        fg = fg,
                        onClick = pane.onClick,
                        content = {
                            if (pane.content == null) {
                                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    if (pane.icon != null) {
                                        Icon(
                                            painter = painterResource(pane.icon),
                                            contentDescription = null,
                                            tint = fg,
                                        )
                                    }
                                    Text(pane.text.orEmpty(), color = fg)
                                }
                            } else {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    pane.content()
                                }
                            }
                        }
                    )

                }
            }
        )
    }
}