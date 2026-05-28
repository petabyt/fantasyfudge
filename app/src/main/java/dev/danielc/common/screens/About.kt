package dev.danielc.common.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ui.theme.FudgeTheme

@Composable
fun About() {
    val uriHandler = LocalUriHandler.current
    data class Dep(
        val name: String,
        val url: String,
        val license: String,
    )

    val deps = listOf(
        Dep("libfuji", "https://github.com/petabyt/libfuji", "MIT License"),
        Dep("ezxml", "https://ezxml.sourceforge.net/", "MIT License"),
        Dep("libjpeg-turbo", "https://github.com/libjpeg-turbo/libjpeg-turbo", "IJG License, Modified (3-clause) BSD License"),
        Dep("quickjs", "https://github.com/bellard/quickjs", "MIT license"),
        Dep("Webassembly Micro Runtime (WAMR)", "https://github.com/bytecodealliance/wasm-micro-runtime", "Apache 2.0 License"),
        Dep("app icon courtesy of Sincerely Media", "https://unsplash.com/photos/a-stack-of-three-pieces-of-food-sitting-on-top-of-a-table-D1zuILDUNzc", "Upsplash License"),
        // TODO: Automate adding android deps
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(deps) { dep ->
            Surface(Modifier.fillMaxWidth().clickable(onClick = {
                uriHandler.openUri(dep.url)
            }), color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(dep.name)
                        Text(dep.license)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun AboutScreen(navController: NavHostController = rememberNavController()) {
    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("About")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigateUp()
                        }) {
                            Icon(painterResource(R.drawable.outline_arrow_back_24), contentDescription = null)
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${stringResource(R.string.app_name)} - based on Fudge (2023-2025)")
                Text("Copyright (C) ${stringResource(R.string.app_name)} 2026, license TBD")
                About()
            }
        }
    }
}