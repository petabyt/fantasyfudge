/// Main app start screen
package dev.danielc.common.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.danielc.R
import dev.danielc.common.ConnectableDevice
import dev.danielc.common.ModuleManifest
import dev.danielc.common.Runtime
import dev.danielc.common.SerializableModuleInstance
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.fudge.AndroidRuntime
import dev.danielc.libpak.Bluetooth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val dummyConnectableDeviceList: List<ConnectableDevice> = listOf(
    ConnectableDevice("Daniel's Earbuds", manifest = dummyManifestList[0], target = dummyManifestList[0].targets[0], isConnected = true),
    ConnectableDevice("Samsung TV", isConnected = false)
)

@Composable
fun ConnectableDeviceCard(dev: ConnectableDevice, clicked: (String?) -> Unit = {}) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .combinedClickable(
            enabled = dev.target != null,
            onClick = {
                clicked(null)
            },
            onLongClick = {
                clicked(null)
            }
        )
        .padding(16.dp)
        .alpha(if (dev.target == null) 0.5f else 1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (dev.target != null) {
                        Icon(
                            painterResource(dev.target.deviceId.getIcon()),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "'${dev.name}'",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                }
               if (dev.isConnected) {
                   Text(
                       text ="Connected",
                       style = MaterialTheme.typography.bodyMedium,
                       color = Color.Green,
                       maxLines = 2,
                   )
               }
               if (dev.target == null) {
                   Text(
                       text ="Not supported",
                       style = MaterialTheme.typography.bodyMedium,
                       color = MaterialTheme.colorScheme.error,
                       maxLines = 2,
                   )
               }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModuleDeviceList(modifier: Modifier = Modifier, deviceList: List<ConnectableDevice>, manifestList: List<ModuleManifest>, clicked: (ModuleManifest, String?) -> Unit) {
    var isRefreshing by remember { mutableStateOf(false) }
    var mutManifestList by remember { mutableStateOf(manifestList) }
    var mutDevList by remember { mutableStateOf(deviceList) }
    val scope = rememberCoroutineScope()
    PullToRefreshBox(
        state = rememberPullToRefreshState(),
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                Runtime.refreshManifests()
                mutManifestList = Runtime.moduleManifests
                Runtime.refreshConnectableDevices()
                mutDevList = Runtime.connectableDevices
                isRefreshing = false
            }
        },
        modifier = modifier
    ) {
        Column(Modifier
            .fillMaxSize()
            .padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mutDevList.isNotEmpty()) {
                Text("Found the following devices nearby:")
                AnimatedContent(
                    targetState = mutDevList,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(3000)
                        )togetherWith fadeOut(animationSpec = tween(3000))
                    },
                    label = "ContentRefresh"
                ) { targetItems ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(targetItems) { dev ->
                            ConnectableDeviceCard(dev)
                        }
                    }
                }
            }
            Text("Select a type of device to connect to:")
            AnimatedContent(
                targetState = mutManifestList,
                transitionSpec = {
                    fadeIn(animationSpec = tween(3000)
                    ) togetherWith fadeOut(animationSpec = tween(3000))
                },
                label = "ContentRefresh"
            ) { targetItems ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(targetItems) { manifest ->
                        for (target in manifest.targets) {
                            TargetCard(target, manifest, clicked = { product ->
                                clicked(manifest, product)
                            })
                        }
                    }
                }
            }
        }
    }
}

//@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun PreviewModuleDeviceList() {
    FudgeTheme {
        Scaffold { innerPadding ->
            ModuleDeviceList(
                Modifier.fillMaxSize().padding(innerPadding),
                manifestList = dummyManifestList,
                deviceList = dummyConnectableDeviceList,
                clicked = { manifest, string -> }
            )
        }
    }
}

@Composable
fun MainNav(navController: NavHostController) {
    var currentLocalGallery by remember { mutableStateOf<LocalGalleryViewModel?>(null) }
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

        navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(navController, goToLocalGallery = {
                navController.navigate("local-gallery")
                CoroutineScope(Dispatchers.IO).launch {
                    currentLocalGallery = LocalGalleryViewModel(AndroidRuntime.getDownloadDirectory())
                    currentLocalGallery?.start()
                }
            })
        }
        composable("help") {
            HelpScreen(navController)
        }
        composable("about") {
            AboutScreen(navController)
        }
        composable("modules-list") {
            ModuleListScreen(navController)
        }
        composable<SerializableModuleInstance> { backStackEntry ->
            val inst = backStackEntry.toRoute<SerializableModuleInstance>()
            val module = inst.getModuleInstance()
            ModuleInstanceNav(module, backToMainScreen = {
                navController.popBackStack()
            })
        }
        composable("local-gallery") {
            fun back() {
                navController.popBackStack()
                CoroutineScope(Dispatchers.IO).launch {
                    currentLocalGallery?.stop()
                    currentLocalGallery = null
                }
            }
            BackHandler {
                back()
            }
            LocalGallery(onBack = {
                back()
            }, onItemClick = { i ->

            }, Modifier, currentLocalGallery)
        }
        composable("gallery") { PreviewGalleryScreen(navController) }
        composable("preview-viewer") { PreviewViewer(navController) }
        composable("test-dashboard1") { PreviewDashboardCamera() }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavHostController = rememberNavController(), goToLocalGallery: () -> Unit = {}) {
    val subNavController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by subNavController.currentBackStackEntryAsState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    data class NavItem(
        val icon: Int,
        val text: String,
        val route: String,
    )
    val items = listOf(
        NavItem(R.drawable.outline_devices_other_24, "Connect", "connect"),
        NavItem(R.drawable.outline_deployed_code_24, "Modules", "modules"),
        NavItem(R.drawable.baseline_terminal_24, "Console", "console"),
    )

    return FudgeTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("FantasyFudge", modifier = Modifier.padding(16.dp))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = {
                            Icon(painter = painterResource(R.drawable.baseline_help_24), contentDescription = null)
                        },
                        label = { Text(text = "Help") },
                        selected = false,
                        onClick = {
                            navController.navigate("help")
                        }
                    )
                    NavigationDrawerItem(
                        icon = {
                            Icon(painter = painterResource(R.drawable.baseline_bug_report_24), contentDescription = null)
                        },
                        label = { Text(text = "Send Feedback") },
                        selected = false,
                        onClick = {
                            uriHandler.openUri("https://danielc.dev/")
                        }
                    )
                    NavigationDrawerItem(
                        icon = {
                            Icon(painter = painterResource(R.drawable.outline_info_24), contentDescription = null)
                        },
                        label = { Text(text = "About") },
                        selected = false,
                        onClick = {
                            navController.navigate("about")
                        }
                    )
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(text = "Preview viewer") },
                        selected = false,
                        onClick = {
                            navController.navigate("preview-viewer")
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(text = "view dashboard") },
                        selected = false,
                        onClick = {
                            navController.navigate("test-dashboard1")
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(),
                        title = {
                            Text("FantasyFudge")
                        },
                        actions = {
                            IconButton(onClick = {
                                goToLocalGallery()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_folder_open_24),
                                    contentDescription = null
                                )
                            }
                            IconButton(onClick = {}) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_settings_24),
                                    contentDescription = null
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            }) {
                                Icon(painterResource(R.drawable.outline_menu_24), contentDescription = "Menu")
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        items.forEach { item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(item.icon),
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(item.text)
                                },
                                selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true,
                                onClick = {
                                    subNavController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                LaunchedEffect(true) {
                    Runtime.logGlobalLine("Hello")
                }
                NavHost(
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    modifier = Modifier.padding(innerPadding),
                    navController = subNavController, startDestination = "connect"
                ) {
                    composable("connect") {
                        ModuleDeviceList(
                            deviceList = Runtime.connectableDevices,
                            manifestList = Runtime.moduleManifests,
                            clicked = { manifest, product ->
                            val mod = Runtime.createModuleInstance(manifest)
                            navController.navigate(mod.serializableModuleInstance)
                            mod.initThread()
                        })
                    }
                    composable("modules") {
                        ModuleList(manifestList = Runtime.moduleManifests)
                    }
                    composable("console") {
                        val state by Runtime.mainLog.uiState.collectAsStateWithLifecycle()
                        Console(state)
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, device = "id:pixel_9a", uiMode = 32)
fun PreviewMainScreen() {
    Runtime.moduleManifests = dummyManifestList as MutableList<ModuleManifest>
    MainScreen()
}