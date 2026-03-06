/// Main app start screen
package dev.danielc.common.screens

import android.content.res.Configuration
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ModuleManifest
import dev.danielc.common.Runtime
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.launch

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
                        ModuleDeviceList(manifestList = dev.danielc.common.Runtime.moduleManifests, clicked = { manifest, product ->
                            val mod = dev.danielc.common.Runtime.createModuleInstance(manifest)
                            navController.navigate(mod.serializableModuleInstance)
                            mod.initThread()
                        })
                    }
                    composable("modules") {
                        ModuleList(manifestList = dev.danielc.common.Runtime.moduleManifests)
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