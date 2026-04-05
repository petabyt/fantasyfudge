package dev.danielc.common.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.ModuleManifest
import dev.danielc.common.Screen
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.plus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.danielc.common.ModuleInstance
import dev.danielc.common.ModuleProperty
import dev.danielc.common.UserSetting
import dev.danielc.common.ui.DisconnectDialog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking

data class HomeState(
    val supportedNavBarScreenList: List<Screen> = emptyList(),
    val supportedMainScreenList: List<Screen> = emptyList(),
    val showDisconnectDialog: Boolean = false,
)

data class UiEvent(
    val type: UiEventType,
    val screen: Screen = Screen.NONE,
) {
    enum class UiEventType {
        SWITCH_SCREEN,
        SWITCH_NAV,
        GO_BACK_SCREEN,
        GO_BACK_NAV,
    }
}

class ModuleHomeModel(val manifest: ModuleManifest, val module: ModuleInstance) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        runBlocking {
            module.deregister()
        }
    }

    private val _dashboardState = MutableStateFlow(DashboardState(manifest))
    val dashboardState = _dashboardState.asStateFlow()

    val dashboardCallbacks: DashboardCallbacks = DashboardCallbacks(
        updateSettingPane = { pane, value ->
            updateSettingPane(pane, value)
        },
        disconnect = {
            showDisconectDialog(true)
        }
    )

    private val _viewerState = MutableStateFlow(ViewerState())
    val viewerState = _viewerState.asStateFlow()

    private val _homeState = MutableStateFlow(HomeState())
    val homeState = _homeState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>(replay = 1)
    val uiEvents = _uiEvents.asSharedFlow()

    fun showDisconectDialog(v: Boolean) {
        _homeState.update { homeState ->
            homeState.copy(
                showDisconnectDialog = v
            )
        }
    }

    fun isScreenInNavBar(s: Screen): Boolean {
        return when (s) {
            Screen.DASHBOARD, Screen.FILE_GALLERY, Screen.LIVEVIEW -> true
            else -> false
        }
    }

    fun uiEvent(type: UiEvent.UiEventType, screen: Screen = Screen.NONE) {
        viewModelScope.launch {
            _uiEvents.emit(UiEvent(type, screen))
        }
    }

    fun goToScreen(screen: Screen, isInHome: Boolean = false) {
        uiEvent(if (isInHome) UiEvent.UiEventType.SWITCH_NAV else UiEvent.UiEventType.SWITCH_SCREEN, screen)
    }

    fun back(isInNavBar: Boolean = false) {
        uiEvent(if (isInNavBar) UiEvent.UiEventType.GO_BACK_NAV else UiEvent.UiEventType.GO_BACK_SCREEN, Screen.NONE)
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

    fun setSupportedScreen(s: Screen, v: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            _homeState.update { currentState ->
                if (!v && currentState.supportedNavBarScreenList.contains(s)) {
                    currentState.copy(supportedNavBarScreenList = currentState.supportedNavBarScreenList.filter { x -> x != s })
                } else if (v && !currentState.supportedNavBarScreenList.contains(s)) {
                    currentState.copy(supportedNavBarScreenList = currentState.supportedNavBarScreenList + s)
                } else {
                    currentState
                }
            }
        }
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

/// Contains main instance navigation bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleHomeScreen(module: ModuleInstance, hostNavController: NavController) {
    val model = module.homeModelView
    val navController = rememberNavController()
    val homeState by model.homeState.collectAsStateWithLifecycle()
    val dashboardState by model.dashboardState.collectAsStateWithLifecycle()
    val galleryState by module.galleryViewModel.uiState.collectAsStateWithLifecycle()
    val navScreens = mutableListOf(
        Screen.DASHBOARD,
    )
    if (homeState.supportedNavBarScreenList.contains(Screen.FILE_GALLERY)) navScreens += Screen.FILE_GALLERY
    if (homeState.supportedNavBarScreenList.contains(Screen.LIVEVIEW)) navScreens += Screen.LIVEVIEW

    LaunchedEffect(Unit) {
        module.homeModelView.uiEvents.collect { event ->
            if (event.type == UiEvent.UiEventType.SWITCH_NAV) {
                //if (event.screen == Screen.FILE_GALLERY || event.screen == Screen.DASHBOARD) {
                navController.navigate(route = event.screen.strId)
                //}
            } else if (event.type == UiEvent.UiEventType.GO_BACK_NAV) {
                navController.navigateUp()
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val haptic = LocalHapticFeedback.current
    return FudgeTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    windowInsets = NavigationBarDefaults.windowInsets,
                    containerColor = TopAppBarDefaults.topAppBarColors().containerColor
                ) {
                    for (i in navScreens.indices) {
                        NavigationBarItem(
                            selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == navScreens[i].strId } == true,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                module.switchScreen(navScreens[i], isInNavBar = true)
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
            fun goBack() {
                val previousRoute = navController.previousBackStackEntry?.destination?.route
                if (previousRoute == null) {
                    model.showDisconectDialog(true)
                } else {
                    val previous = Screen.fromStrId(previousRoute)!!
                    module.goBack(previous, isInNavBar = true)
                }
            }
            NavHost(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                navController = navController, startDestination = Screen.DASHBOARD.strId) {
                composable(Screen.DASHBOARD.strId) {
                    BackHandler { goBack() }
                    Dashboard(Modifier.padding(innerPadding), navController, state = dashboardState, callbacks = model.dashboardCallbacks)
                }
                composable(Screen.FILE_GALLERY.strId) {
                    BackHandler { goBack() }
                    Gallery(navController, innerPadding, galleryState, requestLoad = {})
                }
                composable(Screen.LIVEVIEW.strId) {
                    BackHandler { goBack() }
                    Liveview(Modifier.padding(innerPadding), navController, LiveviewState())
                }
            }
            if (homeState.showDisconnectDialog) {
                DisconnectDialog(dashboardState.nameOfDevice ?: "Device", yes = {
                    module.disconnect("Manual disconnect")
                    model.showDisconectDialog(false)
                },
                no = {
                    model.showDisconectDialog(false)
                })
            }
        }
    }
}

/**
 * Main navigation graph of module instance UI
 */
@Composable
fun ModuleInstanceNav(instance: ModuleInstance, backToMainScreen: () -> Unit = {}) {
    val navController = rememberNavController()
    val viewerState = instance.homeModelView.viewerState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        instance.homeModelView.uiEvents.collect { event ->
            if (event.type == UiEvent.UiEventType.SWITCH_SCREEN) {
                if (event.screen == Screen.DASHBOARD) {
                    navController.navigate("home") {
                        // discard entire nav graph
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                } else if (event.screen == Screen.DISCONNECTED) {
                    navController.navigate("disconnected") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }

    val duration = 200
    NavHost(
        enterTransition = {
            slideIn(
                initialOffset = { IntOffset(it.width, 0) },
                animationSpec = tween(duration, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOut(
                targetOffset = { IntOffset(-it.width / 4, 0) },
                animationSpec = tween(duration, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideIn(
                initialOffset = { IntOffset(-it.width / 4, 0) },
                animationSpec = tween(duration, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOut(
                targetOffset = { IntOffset(it.width, 0) },
                animationSpec = tween(duration, easing = FastOutSlowInEasing)
            )
        },
        navController = navController, startDestination = "connecting") {
        composable("connecting") {
            val state by instance.debugLog.uiState.collectAsStateWithLifecycle()
            ConsoleScreen(navController, state, title = "Connecting")
        }
        composable("home") {
            ModuleHomeScreen(instance, navController)
        }
        composable("viewer") {
            ViewerScreen(viewerState.value, switchTo = { i ->
                println("switching to ${i}")
//                state = state.copy(
//                    filename = "DSCF0002.JPG",
//                    indexInItems = i,
//                    isLoading = true
//                )
            }, close = {
                navController.navigateUp()
            })
        }
        composable("disconnected") {
            val state by instance.debugLog.uiState.collectAsStateWithLifecycle()
            DisconnectedScreen(reason = "Reason: TODO", backToMainScreen, info = {
                Console(state)
            })
        }
    }
}