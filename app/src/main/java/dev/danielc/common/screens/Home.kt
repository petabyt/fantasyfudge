package dev.danielc.common.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.Module
import dev.danielc.common.Screen
import dev.danielc.common.SerializableModuleInstance
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.plus
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielc.common.temporaryManifestList

data class HomeState(
    val supportsLiveView: Boolean = false,
    val supportsGallery: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    var pageIndex: Int = 0,
)

class HomeViewModel(val manifest: Module.Manifest, val module: SerializableModuleInstance? = null) : ViewModel() {
    val dashboard = MutableStateFlow(DashboardState(manifest))
    val dashboardCallbacks: DashboardCallbacks = DashboardCallbacks(updateSettingPane = { pane, value ->
        updateSettingPane(pane, value)
    })
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    fun updateSettingPane(pane: DashboardSettingPane, value: Any) {
        dashboard.value.copy(customSettings = dashboard.value.customSettings.map { curr ->
            if (curr == pane) {
                if (value is Boolean) {
                    curr.copy(currentBooleanValue = value)
                } else {
                    curr
                }
            } else {
                curr
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController = rememberNavController(), state: HomeViewModel) {
    val homestate by state.state.collectAsStateWithLifecycle()
    val dashboardstate by state.dashboard.collectAsStateWithLifecycle()
    val navScreens = mutableListOf(
        Screen.DASHBOARD,
    )
    if (homestate.supportsGallery) navScreens += Screen.FILE_GALLERY
    if (homestate.supportsLiveView) navScreens += Screen.LIVEVIEW

    return FudgeTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    windowInsets = NavigationBarDefaults.windowInsets,
                    containerColor = TopAppBarDefaults.topAppBarColors().containerColor
                ) {
                    for (i in navScreens.indices) {
                        NavigationBarItem(
                            selected = homestate.pageIndex == i,
                            onClick = {
                                homestate.copy(
                                    pageIndex = i
                                )
                                navController.navigate(route = navScreens[homestate.pageIndex].id)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(navScreens[i].getIcon()),
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(navScreens[i].getName())
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                navController = navController, startDestination = Screen.DASHBOARD.id) {
                composable(Screen.DASHBOARD.id) {
                    Dashboard(Modifier.padding(innerPadding), navController, state = state.dashboard.collectAsState().value, callbacks = state.dashboardCallbacks)
                }
                composable(Screen.FILE_GALLERY.id) {
                    Gallery(navController, innerPadding, GalleryState())
                }
                composable(Screen.LIVEVIEW.id) {
                    Liveview(Modifier.padding(innerPadding), navController, LiveviewState())
                }
            }
            if (homestate.showDisconnectDialog) {
                AlertDialog(
                    title = {
                        Text(text = "Disconnect")
                    },
                    text = {
                        Text(text = "Disconnect from ${dashboardstate.nameOfDevice}?")
                    },
                    onDismissRequest = {

                    },
                    confirmButton = {
                        TextButton(
                            onClick = {

                            }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {

                            }
                        ) {
                            Text("No")
                        }
                    }
                )
            }
        }
    }
}