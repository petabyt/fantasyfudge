package dev.danielc.common.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.R
import dev.danielc.common.ui.theme.primaryIconButtonColors

@Composable
fun Intervalometer() {
    Column(Modifier.padding(10.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        var shotsToTake by remember { mutableStateOf("10") }
        var secondsInBetweenShots by remember { mutableStateOf("10") }
        TextField(
            leadingIcon = {
                Icon(painterResource(R.drawable.outline_numbers_24), contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            value = shotsToTake,
            onValueChange = { shotsToTake = it },
            label = { Text("How many shots to take") }
        )
        TextField(
            leadingIcon = {
                Icon(painterResource(R.drawable.outline_watch_later_24), contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            value = secondsInBetweenShots,
            onValueChange = { secondsInBetweenShots = it },
            label = { Text("Seconds inbetween each shot") }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(
                modifier = Modifier.size(100.dp),
                colors = primaryIconButtonColors(),
                onClick = {

                }
            ) {
                Icon(painterResource(R.drawable.outline_shutter_speed_24), contentDescription = null, modifier = Modifier.size(50.dp))
            }
            IconButton(
                modifier = Modifier.size(100.dp),
                colors = primaryIconButtonColors(),
                onClick = {

                }
            ) {
                Icon(painterResource(R.drawable.outline_camera_24), contentDescription = null, modifier = Modifier.size(50.dp))
            }
        }

        Button(onClick = {

        }) {
            Text("Stop")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = 32)
@Composable
fun IntervalometerScreen(navController: NavHostController = rememberNavController()) {
    return FudgeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text("Shutter")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigateUp()
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
            Column(Modifier.padding(innerPadding)) {
                Intervalometer()
            }
        }
    }
}