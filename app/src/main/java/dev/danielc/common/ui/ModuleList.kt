package dev.danielc.common.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.Device
import dev.danielc.common.ModuleManifest
import dev.danielc.common.Runtime
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.errorIconButtonColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val dummyManifestList: List<ModuleManifest> = listOf(
    ModuleManifest(name = "libfuji", description = "libfuji", targets = listOf(ModuleManifest.Target(deviceId = Device.PROFESSIONAL_CAMERA, "Fujifilm", "All Fujifilm cameras", listOf("x-t1", "x-t2", "x-t3", "x-t4", "x-t5"))), isDraft = true),
    ModuleManifest(name = "libgphoto2", description = "ptp2 from the libgphoto2 project", targets = listOf(ModuleManifest.Target(Device.PROFESSIONAL_CAMERA, "Canon", "Canon DSLRs and mirrorless cameras", listOf("EOS 5D", "EOS 5D II", "EOS 5D III"))), isDraft = true),
    ModuleManifest(name = "veement", description = "Veement/veecar", targets = listOf(ModuleManifest.Target(Device.DASHCAM, "Veement", "Veement dashcams"), ModuleManifest.Target(deviceId = Device.DASHCAM, company = "FITCAMX"))),
    ModuleManifest(name = "toyota", description = "Toyota infotainment system", targets = listOf(ModuleManifest.Target(Device.AUTOMOTIVE_INFOTAINMENT, "Toyota", "Toyota infotainment system"))),
    ModuleManifest(name = "libroku", description = "Roku TV and media systems", targets = listOf(ModuleManifest.Target(Device.SMART_TV, "Roku", "Roku TV and media systems"))),
)

@Preview(showBackground = true, device = "id:pixel_9", uiMode = 32)
@Composable
fun ManifestInfoDialog(manifest: ModuleManifest = dummyManifestList[0], close: () -> Unit = {}) {
    Dialog(onDismissRequest = {
        close()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp)) {
                Text(
                    text = manifest.name,
                    modifier = Modifier,
                    style = TextStyle(
                        fontSize = 20.sp
                    ),
                )
                if (manifest.author != null) Text("Author: ${manifest.author}")
                if (manifest.description != null) Text("Description: ${manifest.description}")
                if (manifest.targets.isNotEmpty()) {
                    Text("Targets:")
                    Column(Modifier.padding(horizontal = 5.dp)) {
                        for (e in manifest.targets) {
                            Text("Company: ${e.company}")
                            Text("Type: ${e.deviceId.id}")
                            if (e.products.isNotEmpty()) {
                                Text("Products: " + e.products.joinToString(", "), overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModuleCard(manifest: ModuleManifest, info: () -> Unit, delete: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .clickable(onClick = {
            info()
        })
        .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (manifest.targets.isNotEmpty()) {
                        Icon(
                            painter = painterResource(manifest.targets[0].deviceId.getIcon()),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = manifest.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                            if (manifest.isDraft) {
                                Text(
                                    text = "WIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
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
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    if (manifest.author != null) Text(
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
            Column() {
                IconButton(onClick = {
                    delete()
                }, colors = errorIconButtonColors()) {
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
    var selectedManifest by remember { mutableStateOf<ModuleManifest?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val manifest = selectedManifest
    if (manifest != null) {
        ManifestInfoDialog(manifest, {
            selectedManifest = null
        })
    }

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
                items(manifestList) { manifest ->
                    ModuleCard(manifest,
                        info = {
                            selectedManifest = manifest
                        },
                        delete = {

                        }
                    )
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
                .consumeWindowInsets(innerPadding), dummyManifestList)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
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
                    if (manifest.isDraft) {
                        Text(
                            text = "WIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (target.summary != null) {
                    Text(
                        text = target.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}