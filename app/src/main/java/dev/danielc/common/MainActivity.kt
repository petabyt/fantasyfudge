package dev.danielc.common

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.danielc.R
import dev.danielc.common.screens.ConsoleScreen
import dev.danielc.common.screens.PreviewDashboardBuds
import dev.danielc.common.screens.PreviewGalleryScreen
import dev.danielc.common.screens.PreviewDashboardCamera
import dev.danielc.common.screens.PreviewViewer
import dev.danielc.common.ui.theme.FudgeTheme

const val TAG = "main";

@Composable
fun BottomLog(modifier: Modifier, text: String): Unit {
    if (text.isNotEmpty()) {
        Text(
            text.trim(),
            fontFamily = FontFamily.Monospace,
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(5.dp)
        )
    }
}

@Composable
fun ModuleCard(
    manifest: Module.Manifest,
) {
    Box(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = {

            })
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (manifest.target != null) {
                    Icon(
                        painter = painterResource(manifest.target.deviceId.getIcon()),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = manifest.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    if (manifest.description != null) {
                        Text(
                            text = manifest.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Author: ${manifest.author}\n",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DeviceCard(
    manifest: Module.RememberedDevice,
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .clickable(onClick = {

        })
        .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = manifest.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = manifest.uniqueId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
//@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSelectorScreen(navController: NavHostController = rememberNavController()) {
    val devices: List<Module.Manifest> = listOf(
        Module.Manifest(name = "Fujifilm", description = "Connect to Fujifilm cameras", target = Module.Target(deviceId = Module.Device.PROFESSIONAL_CAMERA)),
        Module.Manifest(name = "Canon", description = "Canon DSLRs and mirrorless cameras", target = Module.Target(deviceId = Module.Device.PROFESSIONAL_CAMERA)),
        Module.Manifest(name = "Veement", description = "Veement/veecar dashcams", target = Module.Target(deviceId = Module.Device.DASHCAM)),
        Module.Manifest(name = "Toyota", description = "Toyota infotainment system", target = Module.Target(deviceId = Module.Device.AUTOMOTIVE_INFOTAINMENT)),
        Module.Manifest(name = "Roku", description = "Roku TV and media systems", target = Module.Target(deviceId = Module.Device.SMART_TV)),
    )

    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("Select a Device")
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_folder_open_24),
                                contentDescription = "Localized description"
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_settings_24),
                                contentDescription = "Localized description"
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column {
                // something here
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(10.dp)) {
                    devices.forEach { dev ->
                        ModuleCard(dev)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModuleList(navController: NavHostController = rememberNavController()) {
    val devices = Runtime.modules
    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("Modules")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigateUp()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            var isRefreshing by remember { mutableStateOf(false) }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    isRefreshing = false
                },
            )  {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(10.dp)) {
                    devices.forEach { dev ->
                        ModuleCard(dev.manifest)
                    }
                }
            }
        }
    }
}

@Composable
fun OldFudgeMenu(navController: NavHostController = rememberNavController()) {
    val m = Modifier.fillMaxWidth()
    Widgets.LongClickButton(m, {
        navController.navigate("gallery")
    }, {
        Log.d(TAG, "Long press")
    }, "Connect to a device")
    Widgets.GrayButton(modifier = m, text = "Help", onClick = {
        Log.d(TAG, "Help")
    })
    Widgets.GrayButton(modifier = m, text = "Send Feedback", onClick = {})

    Box(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(16.dp)
    ) {

    }
}

@Composable
fun DeviceListOnCard(innerPadding: PaddingValues, devices: List<Module.Manifest>) {
    Box(modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
        .paint(
            painterResource(id = R.drawable.background),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.background, blendMode = BlendMode.Color)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background.copy(0.8f))
                    .padding(10.dp)
            ) {
                devices.forEach { dev ->
                    ModuleCard(dev)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true, device = "id:pixel_9a", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    return FudgeTheme {
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
                )
            },
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = {
                    navController.navigate("test-dashboard1")
                }) {
                    Text("preview dashboard camera")
                }
                Button(onClick = {
                    navController.navigate("test-dashboard2")
                }) {
                    Text("preview dashboard buds")
                }
                Button(onClick = {
                    navController.navigate("preview-viewer")
                }) {
                    Text("preview viewer")
                }
                Button(onClick = {
                    navController.navigate("console")
                }) {
                    Text("console")
                }
                Button(onClick = {
                    navController.navigate("modules-list")
                }) {
                    Text("modules list")
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NativeRuntime.setupAndroidContext(this)
        if (!NativeRuntime.hasInited) {
            System.loadLibrary("pakit")
            NativeRuntime.init()
            val manifests = NativeRuntime.getAllJsonManifests()
            Runtime.loadModulesFromManifests(manifests)
            NativeRuntime.hasInited = true
        }
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            NavHost(
                enterTransition = { slideIn { IntOffset(it.width / 2, 0) } + fadeIn() },
                exitTransition = { slideOut { IntOffset(-it.width / 2, 0) } + fadeOut() },
                popEnterTransition = { slideIn { IntOffset(-it.width / 2, 0) } + fadeIn() },
                popExitTransition = { slideOut { IntOffset(it.width / 2, 0) } + fadeOut() },
                navController = navController, startDestination = "home") {
                composable("home") { MainScreen(navController) }
                composable("modules-list") {
                    ModuleList(navController)
                }
                composable("testsuite") { ConsoleScreen(navController) }
                composable("gallery") { PreviewGalleryScreen(navController) }
                composable("preview-viewer") { PreviewViewer(navController) }
                composable("console") {
                    ConsoleScreen(navController, Runtime.mainLog, buttons = {
                        Button(onClick = {
                            Runtime.mainLog.addLine("Hello, World")
                        }) {
                            Text("Do a log")
                        }
                    })
                }
                composable("test-dashboard1") { PreviewDashboardCamera(navController) }
                composable("test-dashboard2") { PreviewDashboardBuds(navController) }
                composable<SerializableModuleInstance> { backStackEntry ->
                    val inst = backStackEntry.toRoute<SerializableModuleInstance>()
                    ConsoleScreen(navController, Runtime.mainLog)
                }
            }
        }
    }
}