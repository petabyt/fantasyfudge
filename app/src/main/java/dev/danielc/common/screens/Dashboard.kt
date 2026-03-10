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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
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
import dev.danielc.common.Device
import dev.danielc.common.ModuleManifest
import dev.danielc.common.UserSetting

data class DashboardState(
    val manifest: ModuleManifest?,
    val customSettings: List<UserSetting> = emptyList(),
    val nameOfDevice: String? = null,
    val filesOnStorage: Int? = null,
    val firmwareVersion: String? = null,
    val supportsGeoTag: Boolean = false,
    val supportsLiveView: Boolean = false,
    val supportsGallery: Boolean = false,
    val supportsFirmwareUpdate: Boolean = false,
)

data class DashboardCallbacks(
    val updateSettingPane: (UserSetting, Any) -> Unit = { pane, value -> }
)

fun budsState(): DashboardState {
    val manifest = ModuleManifest(name = "CMF Nothing", description = "Supports ", targets = listOf(ModuleManifest.Target(deviceId = Device.EARBUDS)))
    val state = DashboardState(
        manifest = manifest,
        nameOfDevice = "CMF Buds Pro 2",
        firmwareVersion = "5.0",
        customSettings = listOf(
            UserSetting(
                "Noise cancellation",
                currentBooleanValue = true
            ),
            UserSetting(
                "Bass enhancement",
                currentBooleanValue = false
            )
        )
    )
    return state
}
fun cameraState(): DashboardState {
    val manifest = ModuleManifest(name = "Fujifilm", targets = listOf(ModuleManifest.Target(deviceId = Device.PROFESSIONAL_CAMERA)))
    return DashboardState(
        manifest = manifest,
        nameOfDevice = "Fujifilm X100VI",
        filesOnStorage = 321,
        firmwareVersion = "0.1.0",
        supportsLiveView = true,
        supportsGallery = true,
        supportsFirmwareUpdate = true,
        supportsGeoTag = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDashboardCamera(navController: NavHostController = rememberNavController()) {
    var state by remember { mutableStateOf(cameraState()) }
    return FudgeTheme {
        Scaffold(
            content = { innerPadding ->
                Dashboard(Modifier.padding(innerPadding), state = state, callbacks = DashboardCallbacks())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDashboardBuds(navController: NavHostController = rememberNavController()) {
    var state by remember { mutableStateOf(budsState()) }
    return FudgeTheme {
        Scaffold(
            content = { innerPadding ->
                Dashboard(Modifier.padding(innerPadding), state = state, callbacks = DashboardCallbacks())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("ModifierParameter")
fun FlowRowScope.PaneButton(text: String, icon: Int, bg: Color, fg: Color, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val rippleConfiguration = RippleConfiguration(color = fg, rippleAlpha = RippleAlpha(
        0.16f,
        0.1f,
        0.08f,
        0.4f
    ))

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
        Box(
            modifier = modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .indication(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.padding(20.dp)
                )
                Text(text, color = fg)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
@SuppressLint("ModifierParameter")
fun FlowRowScope.CustomPane(modifier: Modifier = Modifier, bg: Color = MaterialTheme.colorScheme.surfaceContainerHigh, fg: Color = MaterialTheme.colorScheme.onSurface, content: @Composable () -> Unit) {
    val rippleConfiguration = RippleConfiguration(color = fg, rippleAlpha = RippleAlpha(
        0.16f,
        0.1f,
        0.08f,
        0.4f
    ))

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
        Box(
            modifier = modifier
                .weight(1f)
                //.aspectRatio(1.5f)
                .fillMaxRowHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .clickable(onClick = {})
                .indication(
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            Column(Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
fun Dashboard(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController(), state: DashboardState, callbacks: DashboardCallbacks) {
    Column(
        modifier = modifier
            .padding(10.dp),
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
                        Icon(
                            painter = painterResource(R.drawable.outline_battery_android_frame_1_24),
                            contentDescription = null,
                        )
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
        FlowRow(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2) {
            PaneButton("Settings", R.drawable.baseline_settings_24, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
            PaneButton("Save", R.drawable.outline_save_24, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
            PaneButton("Disconnect", R.drawable.outline_close_24, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
            if (state.supportsGeoTag) {
                PaneButton("Geotagging", R.drawable.outline_globe_location_pin_24, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            }
            if (state.supportsFirmwareUpdate) {
                PaneButton("Update Firmware", R.drawable.outline_developer_board_24, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            }

            for (pane in state.customSettings) {
                val booleanValue = pane.currentBooleanValue
                if (booleanValue != null) {
                    CustomPane(content = {
                        Text(pane.name, color = MaterialTheme.colorScheme.onSurface)
                        Switch(booleanValue,
                            onCheckedChange = {
                                callbacks.updateSettingPane(pane, !booleanValue)
                            }
                        )
                    })
                }
            }
        }
    }
}