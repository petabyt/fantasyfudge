package dev.danielc.common.screens

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import dev.danielc.common.Device
import dev.danielc.common.ModuleManifest
import dev.danielc.common.UserSetting
import dev.danielc.common.ui.theme.FudgeRippleConfig

data class DashboardState(
    val manifest: ModuleManifest?,
    val customSettings: List<UserSetting> = emptyList(),
    val batteryLevel: Int? = null,
    val nameOfDevice: String? = null,
    val filesOnStorage: Int? = null,
    val firmwareVersion: String? = null,
    val supportsGeoTag: Boolean = false,
    val supportsLiveView: Boolean = false,
    val supportsGallery: Boolean = false,
    val supportsFirmwareUpdate: Boolean = false,
)

data class DashboardCallbacks(
    val updateSettingPane: (UserSetting, Any) -> Unit = { pane, value -> },
    val disconnect: () -> Unit = { },
)

data class PaneState(
    val color: PaneState.Color = PaneState.Color.SECONDARY,
    val text: String? = null,
    val icon: Int? = null,
    val onClick: () -> Unit = {},
    val content: (@Composable () -> Unit)? = null,
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
        batteryLevel = 78,
        firmwareVersion = "0.1.0",
        supportsLiveView = true,
        supportsGallery = true,
        supportsFirmwareUpdate = true,
        supportsGeoTag = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_9_pro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDashboardCamera() {
    var state by remember { mutableStateOf(cameraState()) }
    return FudgeTheme {
        Scaffold(
            content = { innerPadding ->
                Dashboard(Modifier.padding(innerPadding), state = state, callbacks = DashboardCallbacks())
            }
        )
    }
}

fun budsState(): DashboardState {
    val manifest = ModuleManifest(name = "CMF Nothing", description = "Supports ", targets = listOf(ModuleManifest.Target(deviceId = Device.EARBUDS)))
    val state = DashboardState(
        manifest = manifest,
        nameOfDevice = "CMF Buds Pro 2",
        firmwareVersion = "5.0",
        customSettings = listOf(
            UserSetting(
                "nc", "Noise cancellation",
                currentBooleanValue = true
            ),
            UserSetting(
                "be", "Bass enhancement",
                currentBooleanValue = false
            ),
            UserSetting(
               "s", "Something",
                currentIntValue = 123
            )
        )
    )
    return state
}

@OptIn(ExperimentalMaterial3Api::class)
//@Preview(showBackground = true, device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
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
@SuppressLint("ModifierParameter")
fun DashboardPane(modifier: Modifier = Modifier, bg: Color, fg: Color, content: @Composable () -> Unit, onClick: () -> Unit) {
    CompositionLocalProvider(LocalRippleConfiguration provides FudgeRippleConfig(fg)) {
        Box(
            modifier = modifier
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
fun Dashboard(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController(), state: DashboardState, callbacks: DashboardCallbacks) {
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
                        if (state.batteryLevel != null) {
                            Icon(
                                painter = painterResource(getBatteryStatusIcon(state.batteryLevel)),
                                contentDescription = null,
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.outline_wifi_24),
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
            PaneState(PaneState.Color.SECONDARY, "Settings", R.drawable.baseline_settings_24),
            PaneState(PaneState.Color.SECONDARY, "Save", R.drawable.outline_save_24),
            PaneState(PaneState.Color.ERROR, "Disconnect", R.drawable.outline_close_24, onClick = {
                callbacks.disconnect()
            }),
        )

        if (state.supportsGeoTag) {
            panes += PaneState(PaneState.Color.TERTIARY, "Geotagging", R.drawable.outline_globe_location_pin_24)
        }
        if (state.supportsFirmwareUpdate) {
            panes += PaneState(PaneState.Color.TERTIARY, "Update Firmware", R.drawable.outline_developer_board_24)
        }

        panes += BatteryListPane(listOf(
            PaneBatteryStatus("Left", 47),
            PaneBatteryStatus("Base", 81),
            PaneBatteryStatus("Right", 46)
        ))

        for (pane in state.customSettings) {
            val booleanValue = pane.currentBooleanValue
            if (booleanValue != null) {
                panes += PaneState(PaneState.Color.NEUTRAL, content = {
                    Text(pane.title, color = MaterialTheme.colorScheme.onSurface)
                    Switch(booleanValue,
                        onCheckedChange = {
                            callbacks.updateSettingPane(pane, !booleanValue)
                        }
                    )
                })
            }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                items(panes) { pane ->
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
                        bg = bg,
                        fg = fg,
                        onClick = pane.onClick,
                        content = {
                            if (pane.content == null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(pane.icon!!),
                                        contentDescription = null,
                                        tint = fg,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                    Text(pane.text.orEmpty(), color = fg, modifier = Modifier.padding(10.dp))
                                }
                            } else {
                                Column(Modifier.padding(20.dp)) {
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