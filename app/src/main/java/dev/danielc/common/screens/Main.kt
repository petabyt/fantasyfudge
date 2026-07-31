/// Main app start screen
package dev.danielc.common.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import dev.danielc.common.ModuleInstanceRequest
import dev.danielc.common.ModuleManifest
import dev.danielc.common.Runtime
import dev.danielc.common.SavedDeviceEntity
import dev.danielc.common.ui.DefaultNavHost
import dev.danielc.common.ui.ModuleList
import dev.danielc.common.ui.ModuleListScreen
import dev.danielc.common.ui.TargetCard
import dev.danielc.common.ui.dummyManifestList
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

private val dummyConnectableDeviceList: List<ConnectableDevice> = listOf(
    ConnectableDevice("CMF Buds Pro 2", manifest = dummyManifestList[0], target = dummyManifestList[0].targets[0]),
)

private val dummyOptions = listOf(
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
                    title = {
                        Text("Choose a mode to proceed")
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
            LazyColumn(Modifier.padding(innerPadding).padding(10.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.Top)) {
                items(options) { e ->
                    // TODO: Interesting colors
                    val colors = when (e.name) {
                        "bluetooth" -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
                        else -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    }
                    Surface(
                        onClick = {
                            onClick(e)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.first,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                when (e.name) {
                                    "wifi" -> painterResource(R.drawable.outline_wifi_24)
                                    "bluetooth" -> painterResource(R.drawable.outline_bluetooth_24)
                                    "usb" -> painterResource(R.drawable.outline_usb_24)
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

@Composable
fun ConnectableDeviceCard(dev: ConnectableDevice, clicked: (String?) -> Unit = {}) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .combinedClickable(
            onClick = {
                clicked(null)
            },
            onLongClick = {
                clicked(null)
            }
        )
        .padding(16.dp)
        .alpha(1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        painterResource(dev.target.deviceId.getIcon()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dev.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun SavedDeviceCard(manifest: ModuleManifest, target: ModuleManifest.Target, dev: SavedDeviceEntity, clicked: () -> Unit = {}) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        .combinedClickable(
            onClick = {
                clicked()
            },
            onLongClick = {
                clicked()
            }
        )
        .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(target.deviceId.getIcon()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dev.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
//                    Spacer(Modifier.weight(1f))
//                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color.Green))
//                    Text(
//                        text = "Nearby",
//                        style = MaterialTheme.typography.titleMedium,
//                        maxLines = 1,
//                    )
                }
                val diff = System.currentTimeMillis().milliseconds.toLong(DurationUnit.MILLISECONDS) - dev.lastSeenTimestamp
                Text("Last connected ${diff / 1000 / 60 / 60} hours ago")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModuleDeviceList(modifier: Modifier = Modifier, deviceList: List<ConnectableDevice>, manifestList: List<ModuleManifest>, savedDevicesList: List<SavedDeviceEntity>, clicked: (ModuleInstanceRequest) -> Unit) {
    var isRefreshing by remember { mutableStateOf(false) }
    var mutManifestList by remember { mutableStateOf(manifestList) }
    var mutDevList by remember { mutableStateOf(deviceList) }
    PullToRefreshBox(
        state = rememberPullToRefreshState(),
        isRefreshing = isRefreshing,
        onRefresh = {
            CoroutineScope(Dispatchers.IO).launch {
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

        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mutDevList.isNotEmpty()) {
                    item {
                        Text("Bonded devices:")
                    }
                }
                items(mutDevList) { dev ->
                    ConnectableDeviceCard(dev, clicked = {
                        clicked(ModuleInstanceRequest(
                            dev.manifest.name,
                            dev.manifest.targets.indexOf(dev.target),
                            deviceMacAddress = dev.macAddress,
                        ))
                    })
                }
                if (savedDevicesList.isNotEmpty()) {
                    item {
                        Text("Connect again:")
                    }
                }
                items(savedDevicesList) { dev ->
                    val manifest = Runtime.getManifestFromName(dev.manifestName)
                    val target = manifest?.targets?.getOrNull(dev.targetIndex)
                    if (manifest != null && target != null) {
                        SavedDeviceCard(manifest, target, dev, clicked = {
                            clicked(ModuleInstanceRequest(
                                manifest.name,
                                dev.targetIndex,
                                savedDeviceUniqueId = dev.uniqueIdentifier,
                                chosenSetupOption = dev.setupOption,
                            ))
                        })
                    }
                }
                item {
                    Text("Select a type of device to connect to:")
                }
                val targets = mutableListOf<Pair<ModuleManifest.Target, ModuleManifest>>()
                for (manifest in mutManifestList) {
                    for (target in manifest.targets) {
                        targets += Pair(target, manifest)
                    }
                }
                items(targets) { pair ->
                    TargetCard(pair.first, pair.second, clicked = {
                        clicked(ModuleInstanceRequest(pair.second.name, pair.second.targets.indexOf(pair.first)))
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    val subNavController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by subNavController.currentBackStackEntryAsState()
    val savedDevices by Runtime.savedDevices.collectAsStateWithLifecycle(emptyList())

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
                if (navBackStackEntry?.destination?.route != "local-gallery") {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(),
                        title = {
                            Text("FantasyFudge")
                        },
                        navigationIcon = {
                            Image(
                                painterResource(R.drawable.icon),
                                contentDescription = null,
                                Modifier.size(40.dp).padding(5.dp).clip(
                                    CircleShape
                                )
                            )
                        },
                        actions = {
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
                }
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
                        savedDevicesList = savedDevices,
                        deviceList = Runtime.connectableDevices,
                        manifestList = Runtime.moduleManifests,
                        clicked = { request ->
                            navController.navigate(request)
                        }
                    )
                }
                composable("modules") {
                    ModuleList(manifestList = Runtime.moduleManifests)
                }
                composable("local-gallery") {
                    LocalGallery(onItemClick = { i ->
                        navController.navigate("local-viewer")
                        CoroutineScope(Dispatchers.IO).launch {
                            Runtime.localGalleryViewModel.itemClicked(GalleryObjectReference(i))
                        }
                    }, Modifier, Runtime.localGalleryViewModel)
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, device = "id:pixel_9a", uiMode = 32)
fun PreviewMainScreen() {
    Runtime.savedDevices = MutableStateFlow(listOf(SavedDeviceEntity(
        uniqueIdentifier = "ASD",
        name = "Fujifilm X-T30",
        manifestName = "libfuji",
        targetIndex = 0,
        setupOption = "bluetooth",
        privateData = null,
    )))
    Runtime.moduleManifests = dummyManifestList as MutableList<ModuleManifest>
    MainScreen()
}

@Composable
fun MainNav(navController: NavHostController) {
    LaunchedEffect(Unit) {
        Runtime.trimMemorySignal.collect {
            Runtime.localGalleryViewModel.onTrimMemory()
        }
    }

    DefaultNavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(navController)
        }
        composable("help") {
            HelpScreen(navController)
        }
        composable("console") {
            val state by Runtime.mainLog.uiState.collectAsStateWithLifecycle()
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
            val manifest = Runtime.getManifestFromName(request.manifestName)
            if (manifest == null) {
                throw Error("${request.manifestName} not found in manifest list")
            }
            val target = manifest.targets[request.targetIndex]
            if (target.setupOptions.isNotEmpty() && request.chosenSetupOption == null) {
                InstanceSetup(target.setupOptions, onClick = { opt ->
                    navController.navigate(request.copy(chosenSetupOption = opt.name))
                }, back = {
                    navController.navigateUp()
                })
            } else {
                val model = viewModel(initializer = { ModuleInstanceModel(manifest, request) })
                ModuleInstanceNav(model.module, backToMainScreen = {
                    navController.popBackStack(route = "home", inclusive = false)
                })
            }
        }
        composable("local-viewer") {
            val state by Runtime.localGalleryViewModel.viewer.viewerState.collectAsStateWithLifecycle()
            ViewerScreen(state, { i ->
                CoroutineScope(Dispatchers.IO).launch {
                    Runtime.localGalleryViewModel.itemClicked(GalleryObjectReference(i))
                }
            }, close = {
                Runtime.localGalleryViewModel.viewer.clear()
                navController.popBackStack()
            }, share = {
                Runtime.localGalleryViewModel.viewer.viewerState.value?.handle?.index?.let {
                    Runtime.localGalleryViewModel.share(it)
                }
            })
        }
    }
}