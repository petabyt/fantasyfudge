/// Main app start screen
package dev.danielc.common.screens

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.danielc.R
import dev.danielc.common.ConnectableDevice
import dev.danielc.common.ModuleGalleryViewModel
import dev.danielc.common.ModuleInstanceRequest
import dev.danielc.common.ModuleManifest
import dev.danielc.common.Runtime
import dev.danielc.common.ViewModelReferences
import dev.danielc.common.ui.ModuleList
import dev.danielc.common.ui.ModuleListScreen
import dev.danielc.common.ui.TargetCard
import dev.danielc.common.ui.dummyManifestList
import dev.danielc.common.ui.theme.FudgeRippleConfig
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.fudge.AndroidRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val dummyConnectableDeviceList: List<ConnectableDevice> = listOf(
    ConnectableDevice("Daniel's Earbuds", manifest = dummyManifestList[0], target = dummyManifestList[0].targets[0], isConnected = true),
    ConnectableDevice("Samsung TV", isConnected = false)
)

val dummyOptions = listOf(
    ModuleManifest.SetupOption("wifi", "WiFi"),
    ModuleManifest.SetupOption("bluetooth", "Bluetooth"),
    ModuleManifest.SetupOption("local", "Local Network (Foo Bar Foo Bar)"),
)

@OptIn(ExperimentalMaterial3Api::class)
//@Preview(showBackground = true, device = "id:pixel_9", uiMode = 32)
@Composable
fun InstanceSetup(options: List<ModuleManifest.SetupOption> = dummyOptions, onClick: (ModuleManifest.SetupOption) -> Unit = {}, back: () -> Unit = {}) {
    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("Choose an mode to proceed")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            back()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.outline_arrow_back_24),
                                contentDescription = null
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding).padding(10.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.Top)) {
                for (e in options) {
                    // TODO: Interesting colors
                    val colors = when (e.name) {
                        "bluetooth" -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
                        else -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    }
                    CompositionLocalProvider(LocalRippleConfiguration provides FudgeRippleConfig()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.first)
                                .clickable(onClick = {
                                    onClick(e)
                                })
                                .indication(
                                    indication = ripple(),
                                    interactionSource = remember { MutableInteractionSource() }
                                )
                        ) {
                            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    when (e.name) {
                                        "wifi" -> painterResource(R.drawable.outline_wifi_24)
                                        "bluetooth" -> painterResource(R.drawable.outline_bluetooth_24)
                                        else -> painterResource(R.drawable.outline_general_device_24)
                                    },
                                    contentDescription = null,
                                    tint = colors.second
                                )
                                Text(e.title, color = colors.second)
                            }
                        }
                    }
                }
            }
        }
    }
}

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
fun ModuleDeviceList(modifier: Modifier = Modifier, deviceList: List<ConnectableDevice>, manifestList: List<ModuleManifest>, clicked: (ModuleManifest, ModuleManifest.Target) -> Unit) {
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
        Column(Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            val targets = mutableListOf<Pair<ModuleManifest.Target, ModuleManifest>>()
            for (manifest in mutManifestList) {
                for (target in manifest.targets) {
                    targets += Pair(target, manifest)
                }
            }
            AnimatedContent(
                targetState = targets,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1000)
                    ) togetherWith fadeOut(animationSpec = tween(1000))
                },
                label = "ContentRefresh"
            ) { targetItems ->
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(targetItems) { pair ->
                        TargetCard(pair.first, pair.second, clicked = {
                            clicked(pair.second, pair.first)
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavHostController = rememberNavController(), localGallery: @Composable () -> Unit = {}) {
    val subNavController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by subNavController.currentBackStackEntryAsState()

    data class NavItem(
        val icon: Int,
        val text: String,
        val route: String,
    )
    val items = listOf(
        NavItem(R.drawable.outline_devices_other_24, "Connect", "connect"),
        NavItem(R.drawable.outline_photo_library_24, "Downloads", "local-gallery"),
        NavItem(R.drawable.outline_deployed_code_24, "Modules", "modules"),
    )

    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("FantasyFudge")
                    },
                    navigationIcon = {
                        Icon(painterResource(R.drawable.outline_construction_24), contentDescription = null, modifier = Modifier.padding(5.dp))
                    },
                    actions = {
//                        IconButton(onClick = {
//                            goToLocalGallery()
//                        }) {
//                            Icon(
//                                painter = painterResource(R.drawable.baseline_folder_open_24),
//                                contentDescription = null
//                            )
//                        }
                        IconButton(onClick = {
                            navController.navigate("settings")
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_settings_24),
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar() {
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
                                    launchSingleTop = true
                                    restoreState = false
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
                navController = subNavController, startDestination = "connect", route = "route"
            ) {
                composable("connect") {
                    ModuleDeviceList(
                        deviceList = Runtime.connectableDevices,
                        manifestList = Runtime.moduleManifests,
                        clicked = { manifest, target ->
                            navController.navigate(ModuleInstanceRequest(manifest.name, manifest.targets.indexOf(target)))
                        }
                    )
                }
                composable("modules") {
                    ModuleList(manifestList = Runtime.moduleManifests)
                }
                composable("local-gallery") { backStackEntry ->
                    // Bind viewmodel state to this nav graph so compose can save/restore states
                    val parentEntry = remember(backStackEntry) {
                        subNavController.getBackStackEntry("route")
                    }
                    CompositionLocalProvider(LocalViewModelStoreOwner provides parentEntry) {
                        localGallery()
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

@Composable
fun MainNav(navController: NavHostController) {
    var currentLocalGallery by remember { mutableStateOf<LocalGalleryViewModel?>(null) }
    val duration = 200

    val mainlog: ConsoleViewModel = viewModel(initializer = {
        val vm = ConsoleViewModel(Runtime.earlyConsoleLogs)
        Runtime.mainLog = vm
        vm
    })

    @Composable
    fun localGallery() {
        val viewer: ViewerModel = viewModel()
        currentLocalGallery = viewModel(initializer = { LocalGalleryViewModel(AndroidRuntime.getDownloadDirectory(), viewer) })
        LocalGallery(onItemClick = { i ->
            navController.navigate("local-viewer")
            CoroutineScope(Dispatchers.IO).launch {
                currentLocalGallery?.loadImage(i)
            }
        }, Modifier, currentLocalGallery)
    }

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
            MainScreen(navController, {
                localGallery()
            })
        }
        composable("help") {
            HelpScreen(navController)
        }
        composable("console") {
            val state by mainlog.uiState.collectAsStateWithLifecycle()
            ConsoleScreen({
                navController.navigateUp()
            }, state, "Debug Console")
        }
        composable("about") {
            AboutScreen(navController)
        }
        composable("settings") {
            SettingsScreen(navController)
        }
        composable("modules-list") {
            ModuleListScreen(navController)
        }
        composable<ModuleInstanceRequest> { backStackEntry ->
            val request = backStackEntry.toRoute<ModuleInstanceRequest>()
            val manifest = Runtime.getManifestFromName(request.manifestName)!!
            val target = manifest.targets[request.targetIndex]
            if (target.setupOptions.isNotEmpty() && request.chosenSetupOption == null) {
                InstanceSetup(target.setupOptions, onClick = { opt ->
                    navController.navigate(request.copy(chosenSetupOption = opt.name))
                }, back = {
                    navController.popBackStack()
                })
            } else {
                val models = ViewModelReferences(
                    viewModel(initializer = { ModuleGalleryViewModel() }),
                    viewModel(initializer = { ViewerModel() }),
                    viewModel(initializer = { ConsoleViewModel() })
                )
                val model = viewModel(initializer = { ModuleInstanceModel(manifest, request, models) })
                ModuleInstanceNav(model.module, backToMainScreen = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                })
            }
        }
        composable("local-viewer") {
            currentLocalGallery?.let {
                val state by it.viewer.viewerState.collectAsStateWithLifecycle()
                ViewerScreen(state, { i ->
                    CoroutineScope(Dispatchers.IO).launch {
                        it.loadImage(i)
                    }
                }, {
                    it.viewer.clear()
                    navController.popBackStack()
                })
            }
        }
    }
}