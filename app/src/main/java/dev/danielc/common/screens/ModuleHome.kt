package dev.danielc.common.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.DashboardPane
import dev.danielc.common.FileHandle
import dev.danielc.common.ModuleInstance
import dev.danielc.common.ModuleInstanceRequest
import dev.danielc.common.ModuleManifest
import dev.danielc.common.ModuleProperty
import dev.danielc.common.Runtime
import dev.danielc.common.Screen
import dev.danielc.common.ui.DefaultNavHost
import dev.danielc.common.ui.DisconnectDialog
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class HomeState(
    val supportedNavBarScreenList: List<Screen> = listOf(Screen.DASHBOARD),
    val supportedMainScreenList: List<Screen> = listOf(Screen.DASHBOARD),
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

class ModuleInstanceModel(manifest: ModuleManifest, request: ModuleInstanceRequest) : ViewModel() {
    override fun onCleared() {
        runBlocking {
            module.deregister()
        }
    }

    private val _dashboardState = MutableStateFlow(DashboardState(manifest, connectionType = request.getSetupOption()?.transport))
    val dashboardState = _dashboardState.asStateFlow()
    private val _homeState = MutableStateFlow(HomeState())
    val homeState = _homeState.asStateFlow()
    private val _uiEvents = MutableSharedFlow<UiEvent>(replay = 1)
    val uiEvents = _uiEvents.asSharedFlow()
    val connectProgress = MutableStateFlow<Int?>(null)
    val connectRequiredAction = MutableStateFlow(ConnectingRequiredAction.NONE)
    val initializationError = MutableStateFlow(false)

    var module: ModuleInstance = ModuleInstance(manifest, request, this)
    init {
        module.initThread()
    }

    fun updateNumFiles(files: Int?) {
        _dashboardState.update { state ->
            state.copy(
                filesOnStorage = files
            )
        }
    }

    fun showDisconnectDialog(v: Boolean) {
        _homeState.update { homeState ->
            homeState.copy(
                showDisconnectDialog = v
            )
        }
    }

    fun isScreenInNavBar(s: Screen): Boolean {
        return when (s) {
            Screen.DASHBOARD, Screen.FILE_GALLERY, Screen.LIVEVIEW, Screen.INTERVALOMETER -> true
            else -> false
        }
    }

    fun sendUiEvent(type: UiEvent.UiEventType, screen: Screen = Screen.NONE) {
        viewModelScope.launch {
            _uiEvents.emit(UiEvent(type, screen))
        }
    }

    fun goToScreen(screen: Screen, isInNavBar: Boolean = false) {
        sendUiEvent(if (isInNavBar) UiEvent.UiEventType.SWITCH_NAV else UiEvent.UiEventType.SWITCH_SCREEN, screen)
    }

    fun back(isInNavBar: Boolean = false) {
        sendUiEvent(if (isInNavBar) UiEvent.UiEventType.GO_BACK_NAV else UiEvent.UiEventType.GO_BACK_SCREEN, Screen.NONE)
    }

    fun setProperty(type: ModuleProperty, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _dashboardState.update { currentState ->
                when (type) {
                    ModuleProperty.NAME_OF_DEVICE -> currentState.copy(nameOfDevice = value)
                    ModuleProperty.FIRMWARE_VERSION -> currentState.copy(firmwareVersion = value)
                    else -> currentState
                }
            }
        }
    }
    fun setProperty(type: ModuleProperty, value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _dashboardState.update { currentState ->
                when (type) {
                    ModuleProperty.TEMPERATURE -> currentState.copy(temperature = value)
                    ModuleProperty.HUMIDITY -> currentState.copy(humidity = value)
                    ModuleProperty.BATTERY_LEFT -> currentState.copy(batteryLevelLeft = value)
                    ModuleProperty.BATTERY_MAIN -> currentState.copy(batteryLevelMain = value)
                    ModuleProperty.BATTERY_RIGHT -> currentState.copy(batteryLevelRight = value)
                    else -> currentState
                }
            }
        }
    }

    fun setSupportedScreen(s: Screen, v: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _homeState.update { currentState ->
                // TODO: refactor
                if (isScreenInNavBar(s)) {
                    if (!v && currentState.supportedNavBarScreenList.contains(s)) {
                        currentState.copy(supportedNavBarScreenList = currentState.supportedNavBarScreenList.filter { x -> x != s })
                    } else if (v && !currentState.supportedNavBarScreenList.contains(s)) {
                        currentState.copy(supportedNavBarScreenList = currentState.supportedNavBarScreenList + s)
                    } else {
                        currentState
                    }
                } else {
                    if (!v && currentState.supportedMainScreenList.contains(s)) {
                        currentState.copy(supportedMainScreenList = currentState.supportedMainScreenList.filter { x -> x != s })
                    } else if (v && !currentState.supportedMainScreenList.contains(s)) {
                        currentState.copy(supportedMainScreenList = currentState.supportedMainScreenList + s)
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    fun setDashboardPane(pane: DashboardPane) {
        viewModelScope.launch(Dispatchers.Default) {
            _dashboardState.update { currentState ->
                if (currentState.panes.find { it.args.name == pane.args.name } == null) {
                    currentState.copy(panes = currentState.panes + pane)
                } else {
                    currentState
                }
            }
        }
    }

    fun updateSettingPane(pane: DashboardPane) {
        viewModelScope.launch(Dispatchers.Default) {
            _dashboardState.update { currentState ->
                val index = currentState.panes.find { it.args.name == pane.args.name }
                val list = currentState.panes.toMutableList()
                if (index != null) {
                    list[currentState.panes.indexOf(index)] = pane
                    currentState.copy(panes = list)
                } else {
                    currentState
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                module.setProp(pane)
            }
        }
    }
}

/// Contains main instance navigation bar
@Composable
fun ModuleHomeScreen(module: ModuleInstance, hostNavController: NavController) {
    val model = module.homeModelView
    val navController = rememberNavController()
    val homeState by model.homeState.collectAsStateWithLifecycle()
    val dashboardState by model.dashboardState.collectAsStateWithLifecycle()
    val galleryState by module.galleryViewModel.uiState.collectAsStateWithLifecycle()
    val navScreens = homeState.supportedNavBarScreenList.sortedBy { when (it) {
        Screen.DASHBOARD -> 0 // ensure dashboard is always first
        Screen.FILE_GALLERY -> 1
        else -> 2
    } }
    var screenSwitchProgress by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        module.homeModelView.uiEvents.collect { event ->
            if (event.type == UiEvent.UiEventType.SWITCH_NAV) {
                navController.navigate(route = event.screen.strId)
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
                Box {
                    NavigationBar(
                        windowInsets = NavigationBarDefaults.windowInsets,
                        containerColor = TopAppBarDefaults.topAppBarColors().containerColor
                    ) {
                        for (i in navScreens.indices) {
                            NavigationBarItem(
                                enabled = screenSwitchProgress == null,
                                selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == navScreens[i].strId } == true,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        module.switchScreen(navScreens[i], isInNavBar = true, { job ->
                                            screenSwitchProgress = job.progressBarValue
                                        })
                                        screenSwitchProgress = null
                                    }
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

                    screenSwitchProgress?.let {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            progress = { it.toFloat() / 100 }
                        )
                    }
                }
            }
        ) { innerPadding ->
            fun goBack() {
                val previousRoute = navController.previousBackStackEntry?.destination?.route
                if (previousRoute == null) {
                    model.showDisconnectDialog(true)
                } else {
                    val previous = Screen.fromStrId(previousRoute)!!
                    CoroutineScope(Dispatchers.IO).launch {
                        module.goBack(previous, isInNavBar = true, { job ->
                            screenSwitchProgress = job.progressBarValue
                        })
                        screenSwitchProgress = null
                    }
                }
            }
            NavHost(
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                navController = navController, startDestination = Screen.DASHBOARD.strId) {
                composable(Screen.DASHBOARD.strId) {
                    BackHandler { goBack() }
                    Dashboard(Modifier.padding(innerPadding), navController, state = dashboardState, callbacks =
                        DashboardCallbacks(
                            updatePaneValue = { pane ->
                                model.updateSettingPane(pane)
                            },
                            disconnect = {
                                model.showDisconnectDialog(true)
                            },
                            runCommand = { cmd ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    module.runCommand(cmd)
                                }
                            }
                        )
                    )
                }
                composable(Screen.FILE_GALLERY.strId) {
                    BackHandler { goBack() }
                    Gallery(Modifier.padding(innerPadding), galleryState, requestLoad = { items ->
                        module.galleryViewModel.enqueueObjects(items)
                    }, onItemClick = { i ->
                        module.goToViewer(FileHandle(i))
                    })
                }
                composable(Screen.LIVEVIEW.strId) {
                    BackHandler { goBack() }
                    Liveview(Modifier.padding(innerPadding), navController, LiveviewState())
                }
                composable(Screen.INTERVALOMETER.strId) {
                    BackHandler { goBack() }
                    Intervalometer(Modifier.padding(innerPadding), module.intervalometerModel)
                }
            }
            if (homeState.showDisconnectDialog) {
                DisconnectDialog(dashboardState.nameOfDevice ?: "Device", yes = {
                    module.forceDisconnect("Manual disconnect")
                    model.showDisconnectDialog(false)
                },
                no = {
                    model.showDisconnectDialog(false)
                })
            }
        }
    }
}

/**
 * Main navigation graph of module instance UI
 */
@Composable
fun ModuleInstanceNav(module: ModuleInstance, backToMainScreen: () -> Unit = {}) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        module.homeModelView.uiEvents.collect { event ->
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
                } else {
                    navController.navigate(event.screen.strId)
                }
            } else if (event.type == UiEvent.UiEventType.GO_BACK_SCREEN) {
                navController.navigateUp()
            }
        }
    }

    val debugLogState by module.debugLogModel.uiState.collectAsStateWithLifecycle()
    val viewerState by module.viewerViewModel.viewerState.collectAsStateWithLifecycle()

    DefaultNavHost(navController = navController, startDestination = "connecting") {
        composable("connecting-secondary") {
            val connectProgress by module.homeModelView.connectProgress.collectAsStateWithLifecycle()
            val action by module.homeModelView.connectRequiredAction.collectAsStateWithLifecycle()
            ConnectingScreen({
                module.homeModelView.back(false)
            }, {

            }, ConnectingScreenState(
                debugLogState, connectProgress, action, module.target
            ))
        }
        composable("connecting") {
            val connectProgress by module.homeModelView.connectProgress.collectAsStateWithLifecycle()
            val action by module.homeModelView.connectRequiredAction.collectAsStateWithLifecycle()
            val isInitError by module.homeModelView.initializationError.collectAsStateWithLifecycle()
            var hasCancelled by remember { mutableStateOf(false) }
            if (isInitError) {
                ModuleErrorScreen(backToMainScreen, debugLogState)
            } else {
                ConnectingScreen({
                    if (!hasCancelled) {
                        hasCancelled = true
                        CoroutineScope(Dispatchers.IO).launch {
                            module.debugLog("Cancelling...")
                            module.stopAllThreads()
                            CoroutineScope(Dispatchers.Main).launch {
                                backToMainScreen()
                            }
                        }
                    }
                }, {
                    module.tryConnectAgain()
                }, ConnectingScreenState(debugLogState, connectProgress, action, module.target,
                    transport = module.request.getSetupOption()?.transport ?: ModuleManifest.Transport.WIFI_AP)
                )
            }
        }
        composable("home") {
            ModuleHomeScreen(module, navController)
        }
        composable("disconnected") {
            val reason = "${module.disconnectReason ?: "(no reason)"} - (${Runtime.errorCodeToString(module.disconnectedErrorCode ?: 0)})"
            DisconnectedScreen(reason, backToMainScreen = backToMainScreen, consoleState = debugLogState)
        }
        composable(Screen.FILE_VIEWER.strId) {
            ViewerScreen(viewerState,
                switchTo = { i ->
                    module.goToViewer(FileHandle(i, viewerState?.handle?.storageName))
                }, close = {
                    CoroutineScope(Dispatchers.IO).launch {
                        module.goBack(Screen.FILE_GALLERY, false)
                    }
                },
                cancel = {
                    module.viewerDownloadJob?.let {
                        module.cancelJob(it)
                    }
                },
                save = {
                    module.viewerViewModel.onSave()
                }
            )
        }
    }
}