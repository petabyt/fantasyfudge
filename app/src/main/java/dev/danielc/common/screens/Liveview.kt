package dev.danielc.common.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import dev.danielc.common.ui.GrayButton

data class LiveviewState(
    val iso: Int? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "id:pixel_9a", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewLiveview(navController: NavHostController = rememberNavController()) {
    val state = LiveviewState(

    )
    FudgeTheme {
        Scaffold { innerPadding ->
            Liveview(Modifier.padding(innerPadding), state = state)
        }
    }
}

@Composable
fun RowScope.LiveviewButton(text: String, icon: Int, shortText: String) {
    GrayButton(Modifier.weight(1f), onClick = {

    }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(icon),
                tint = Color.White,
                contentDescription = text
            )
            //Text(text, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun Liveview(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController(), state: LiveviewState) {
    Box(modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            LiveviewButton("White Balance", R.drawable.outline_wb_sunny_24, "WB")
            LiveviewButton("ISO", R.drawable.outline_iso_24, "ISO")
            LiveviewButton("Shutter Speed", R.drawable.outline_shutter_speed_24, "SHUTT")
            LiveviewButton("Aperture", R.drawable.baseline_photo_camera_24, "APERT")
            LiveviewButton("Format", R.drawable.outline_style_24, "FORMAT")
        }

        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(R.drawable.image),
            contentDescription = null
        )

        Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            GrayButton(onClick = {

            }) {
                Text("Capture", color = Color.White)
            }
        }
    }
}