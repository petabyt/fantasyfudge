package dev.danielc.common.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.Device
import dev.danielc.common.ModuleManifest
import dev.danielc.common.Runtime
import dev.danielc.common.ui.theme.FudgeTheme
import kotlinx.coroutines.delay
import dev.danielc.R
import dev.danielc.common.ConnectableDevice
import kotlinx.coroutines.launch

val dummyManifestList: List<ModuleManifest> = listOf(
    ModuleManifest(name = "libfuji", description = "All Fujifilm cameras", targets = listOf(ModuleManifest.Target(deviceId = Device.PROFESSIONAL_CAMERA, company = "Fujifilm", listOf("x-t1", "x-t2", "x-t3", "x-t4", "x-t5")))),
    ModuleManifest(name = "canon", description = "Canon DSLRs and mirrorless camerasbalblahblahblablabhb", targets = listOf(ModuleManifest.Target(deviceId = Device.PROFESSIONAL_CAMERA, company = "Canon", listOf("EOS 5D", "EOS 5D II", "EOS 5D III")))),
    ModuleManifest(name = "veement", description = "Veement/veecar dashcams", targets = listOf(ModuleManifest.Target(deviceId = Device.DASHCAM, company = "Veement"))),
    ModuleManifest(name = "toyota", description = "Toyota infotainment system", targets = listOf(ModuleManifest.Target(deviceId = Device.AUTOMOTIVE_INFOTAINMENT, company = "Toyota"))),
    ModuleManifest(name = "libroku", description = "Roku TV and media systems", targets = listOf(ModuleManifest.Target(deviceId = Device.SMART_TV, company = "Roku"))),
)

@Composable
fun ModuleCard(
    manifest: ModuleManifest,
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .clickable(onClick = {

        })
        .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (e in manifest.targets) {
                        Icon(
                            painter = painterResource(e.deviceId.getIcon()),
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
                Column {
                    Text(
                        text = "Author: ${manifest.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Type: ${manifest.moduleType.getDesc()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(colors = IconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ), onClick = {

                }) {
                    Icon(painterResource(R.drawable.outline_info_24), contentDescription = null)
                }
                IconButton(onClick = {}, colors = IconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )) {
                    Icon(painterResource(R.drawable.outline_delete_24), contentDescription = null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModuleList(modifier: Modifier = Modifier, manifestList: List<ModuleManifest>) {
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    PullToRefreshBox(
        state = rememberPullToRefreshState(),
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                Runtime.refreshManifests()
                refreshTrigger++
                delay(10)
                isRefreshing = false
            }
        },
        modifier = modifier
    ) {
        key(refreshTrigger) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                items(manifestList) { dev ->
                    ModuleCard(dev)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun PreviewModuleList() {
    FudgeTheme {
        Scaffold { innerPadding ->
            ModuleList(Modifier
                .fillMaxSize()
                .padding(innerPadding), dummyManifestList)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModuleListScreen(navController: NavHostController = rememberNavController()) {
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
                                painter = painterResource(R.drawable.outline_arrow_back_24),
                                contentDescription = "Back"
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            ModuleList(Modifier.padding(innerPadding), Runtime.moduleManifests)
        }
    }
}

@Composable
fun TargetCard(target: ModuleManifest.Target, manifest: ModuleManifest, clicked: (String?) -> Unit = {}) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .combinedClickable(
            onClick = {
                clicked(null)
            },
            onLongClick = {
                clicked(null)
            }
        )
        .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        painterResource(target.deviceId.getIcon()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = target.company,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                }
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
    }
}