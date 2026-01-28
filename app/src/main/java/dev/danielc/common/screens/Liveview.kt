package dev.danielc.common.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.R
import androidx.compose.ui.graphics.Color
import dev.danielc.common.Widgets

data class LiveviewState(
    val iso: Int? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_9a", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Preview(navController: NavHostController = rememberNavController()) {
    val state = LiveviewState(

    )
    FudgeTheme {
        Scaffold { innerPadding ->
            Liveview(Modifier.padding(innerPadding), state = state)
        }
    }
}

@Composable
fun Liveview(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController(), state: LiveviewState) {
    Box(Modifier.fillMaxHeight()) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(R.drawable.background),
            contentDescription = null
            )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Widgets.GrayButton(onClick = {

            }) {
                Text("WB", color = Color.White)
            }
            Widgets.GrayButton(onClick = {

            }) {
                Text("ISO", color = Color.White)
            }
            Widgets.GrayButton(onClick = {

            }) {
                Text("SHUTT", color = Color.White)
            }
            Widgets.GrayButton(onClick = {

            }) {
                Text("APERT", color = Color.White)
            }
            Widgets.GrayButton(onClick = {

            }) {
                Text("FORMAT", color = Color.White)
            }
        }

        Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Widgets.GrayButton(onClick = {

            }) {
                Text("Capture", color = Color.White)
            }
        }
    }
}