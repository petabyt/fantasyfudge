package dev.danielc.common

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardState(
    val manifest: Module.Manifest?,
    val module: SerializableModuleInstance? = null,
    val nameOfDevice: String? = null,
    val filesOnStorage: Int? = null,
    val firmwareVersion: String? = null,
    val supportsGeoTag: Boolean = false,
    val supportsLiveView: Boolean = false,
    val supportsGallery: Boolean = false,
    val supportsFirmwareUpdate: Boolean = false,
    var pageIndex: Int = 0,
)

class DashboardStateModel(manifest: Module.Manifest) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardState(manifest))
    val dummyState: DashboardState = DashboardState(manifest)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    fun update() {
        viewModelScope.launch() {
            withContext(Dispatchers.Default) {
                _uiState.update { currentState ->
                    dummyState
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDashboardCamera(navController: NavHostController = rememberNavController()) {
    val state = DashboardState(
        manifest = temporaryManifestList[0],
        nameOfDevice = "Fujifilm X100V",
        filesOnStorage = 321,
        firmwareVersion = "0.1.0",
        supportsFirmwareUpdate = true,
        supportsGeoTag = true,
    )
    DashboardScreen(state = state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDashboardBuds(navController: NavHostController = rememberNavController()) {
    val state = DashboardState(
        manifest = temporaryManifestList[1],
        nameOfDevice = "CMF Buds Pro 2",
        firmwareVersion = "5.0",
    )
    DashboardScreen(state = state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("ModifierParameter")
fun FlowRowScope.CardButton(text: String, icon: Int, bg: Color, fg: Color, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {

    val rippleConfiguration = RippleConfiguration(color = fg, rippleAlpha = RippleAlpha(
        0.16f,
        0.1f,
        0.08f,
        0.4f
    )
    )


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

@Composable
fun Dashboard(innerPadding: PaddingValues, navController: NavHostController = rememberNavController(), state: DashboardState) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.nameOfDevice != null) {
                        if (state.manifest != null)
                            Icon(
                                painter = painterResource(state.manifest.target.deviceId.getIcon()),
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
            CardButton("Settings", R.drawable.baseline_settings_24, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
            CardButton("Save", R.drawable.outline_save_24, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
            CardButton("Disconnect", R.drawable.outline_close_24, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
            if (state.supportsGeoTag) {
                CardButton("Geotagging", R.drawable.outline_globe_location_pin_24, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            }
            Box(modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Change a setting", color = MaterialTheme.colorScheme.onSurface)
                    Switch(true,
                        onCheckedChange = {

                        })
                }
            }
            if (state.supportsFirmwareUpdate) {
                CardButton("Update Firmware", R.drawable.outline_developer_board_24, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController = rememberNavController(), state: DashboardState) {
    return FudgeTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    windowInsets = NavigationBarDefaults.windowInsets,
                    containerColor = TopAppBarDefaults.topAppBarColors().containerColor
                ) {
                    NavigationBarItem(
                        selected = state.pageIndex == 0,
                        onClick = {
                            state.pageIndex = 0
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.outline_home_24),
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Dashboard")
                        }
                    )
                    NavigationBarItem(
                        selected = state.pageIndex == 1,
                        onClick = {
                            state.pageIndex = 1
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.outline_photo_library_24),
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Gallery")
                        }
                    )
                    NavigationBarItem(
                        selected = state.pageIndex == 2,
                        onClick = {
                            state.pageIndex = 2
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.outline_smart_display_24),
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Liveview")
                        }
                    )
                }
            }
        ) { innerPadding ->
            Dashboard(innerPadding, navController, state)
        }
    }
}