package dev.danielc.common

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
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
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.danielc.R
import dev.danielc.common.screens.AboutScreen
import dev.danielc.common.screens.Console
import dev.danielc.common.screens.ConsoleScreen
import dev.danielc.common.screens.HelpScreen
import dev.danielc.common.screens.HomeScreen
import dev.danielc.common.screens.ModuleCard
import dev.danielc.common.screens.ModuleDeviceList
import dev.danielc.common.screens.ModuleList
import dev.danielc.common.screens.ModuleListScreen
import dev.danielc.common.screens.PreviewGalleryScreen
import dev.danielc.common.screens.PreviewDashboardCamera
import dev.danielc.common.screens.PreviewViewer
import dev.danielc.common.screens.devices
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.libpak.Bluetooth
import dev.danielc.libpak.Pak
import kotlinx.coroutines.launch

const val TAG = "main";

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
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
                            IconButton(onClick = {}) {
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
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                NavHost(
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    modifier = Modifier.padding(innerPadding),
                    navController = subNavController, startDestination = "connect"
                ) {
                    composable("connect") {
                        ModuleDeviceList(manifestList = Runtime.moduleManifests, clicked = { manifest, product ->
                            val mod = Runtime.createModuleInstance(manifest)
                            navController.navigate(mod.serializableModuleInstance)
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
@Preview(showBackground = true, device = "id:pixel_9a", uiMode = Configuration.UI_MODE_NIGHT_YES)
fun PreviewMainScreen() {
    Runtime.moduleManifests = devices as MutableList<ModuleManifest>
    MainScreen()
}

@Composable
fun ModuleInstanceNav(instance: ModuleInstance) {
    val state by instance.debugLog.uiState.collectAsStateWithLifecycle()

    val duration = 200
    val navController = rememberNavController()
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
        composable("home") { backStackEntry ->
            HomeScreen(instance, navController)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (permissions[0] === "android.permission.BLUETOOTH_CONNECT") {
            Bluetooth.permissionResult(grantResults[0])
        }
        Bluetooth.permissionResult(grantResults[0])
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeRuntime.setupAndroidContext(this)
        Pak.setupAndroidContext(this)
        if (!NativeRuntime.hasInited) {
            System.loadLibrary("fudge")
            NativeRuntime.init()
            val manifests = NativeRuntime.getJsonManifestList()
            Runtime.loadModulesFromManifests(manifests)
            NativeRuntime.hasInited = true
        }
        enableEdgeToEdge()

        setContent {
            val duration = 200
            val navController = rememberNavController()
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
                    MainScreen(navController)
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
                composable("testsuite") { ConsoleScreen(navController) }
                composable("gallery") { PreviewGalleryScreen(navController) }
                composable("preview-viewer") { PreviewViewer(navController) }
                composable("console") {
                    val state by Runtime.mainLog.uiState.collectAsStateWithLifecycle()
                    ConsoleScreen(navController, state = state, buttons = {
                        Button(onClick = {
                            Runtime.mainLog.addLine("Hello, World")
                        }) {
                            Text("Do a log")
                        }
                    })
                }
                composable("test-dashboard1") { PreviewDashboardCamera(navController) }
                composable<SerializableModuleInstance> { backStackEntry ->
                    val inst = backStackEntry.toRoute<SerializableModuleInstance>()
                    ModuleInstanceNav(inst.getModuleInstance())
                }
            }
        }
    }
}