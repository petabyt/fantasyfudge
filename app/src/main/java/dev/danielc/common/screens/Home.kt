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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.ModuleManifest
import dev.danielc.common.Screen
import dev.danielc.common.SerializableModuleInstance
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.plus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielc.common.ModuleInstance
import dev.danielc.common.ModuleProperty
import dev.danielc.common.UserSetting

data class HomeState(
    val supportedScreenList: List<Screen> = emptyList(),
    val showDisconnectDialog: Boolean = false,
    var pageIndex: Int = 0,
)

class HomeViewModel(val manifest: ModuleManifest, val module: SerializableModuleInstance? = null) : ViewModel() {
    private val _dashboardState = MutableStateFlow(DashboardState(manifest))
    val dashboardState = _dashboardState.asStateFlow()
    val dashboardCallbacks: DashboardCallbacks = DashboardCallbacks(updateSettingPane = { pane, value ->
        updateSettingPane(pane, value)
    })
    private val _homeState = MutableStateFlow(HomeState())
    val homeState = _homeState.asStateFlow()

    fun setPageIndex(i: Int) {
        _homeState.update { homeState ->
            homeState.copy(
                pageIndex = i
            )
        }
    }

    fun setProperty(type: ModuleProperty, value: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _dashboardState.update { currentState ->
                when (type) {
                    ModuleProperty.NAME_OF_DEVICE -> currentState.copy(nameOfDevice = value)
                    ModuleProperty.FIRMWARE_VERSION -> currentState.copy(firmwareVersion = value)
                }
            }
        }
    }

    fun addSupportedScreen(s: Screen) {
        viewModelScope.launch(Dispatchers.Default) {
            _homeState.update { currentState ->
                currentState.copy(
                    supportedScreenList = currentState.supportedScreenList + s
                )
            }
        }
    }

    fun addSupportedScreen(s: Int) {
        val screen = Screen.fromId(s)
        if (screen != null) addSupportedScreen(screen)
    }

    fun addSettingPane(pane: UserSetting) {
        viewModelScope.launch(Dispatchers.Default) {
            _dashboardState.update { currentState ->
                currentState.copy(customSettings = currentState.customSettings + pane)
            }
        }
    }

    fun updateSettingPane(pane: UserSetting, value: Any) {
        viewModelScope.launch(Dispatchers.Default) {
            _dashboardState.update { currentState ->
                currentState.copy(customSettings = currentState.customSettings.map { curr ->
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(module: ModuleInstance) {
    val model = module.homeModelView
    val navController = rememberNavController()
    val homestate by model.homeState.collectAsStateWithLifecycle()
    val dashboardstate by model.dashboardState.collectAsStateWithLifecycle()
    val navScreens = mutableListOf(
        Screen.DASHBOARD,
    )
    if (homestate.supportedScreenList.contains(Screen.FILE_GALLERY)) navScreens += Screen.FILE_GALLERY
    if (homestate.supportedScreenList.contains(Screen.LIVEVIEW)) navScreens += Screen.LIVEVIEW

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
                                model.setPageIndex(i)
                                navController.navigate(route = navScreens[i].strId)
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
                navController = navController, startDestination = Screen.DASHBOARD.strId) {
                composable(Screen.DASHBOARD.strId) {
                    Dashboard(Modifier.padding(innerPadding), navController, state = dashboardstate, callbacks = model.dashboardCallbacks)
                }
                composable(Screen.FILE_GALLERY.strId) {
                    Gallery(navController, innerPadding, GalleryState())
                }
                composable(Screen.LIVEVIEW.strId) {
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