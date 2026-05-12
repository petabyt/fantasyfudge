package dev.danielc.common.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ui.theme.FudgeTheme
import dev.danielc.common.ui.theme.LightGray

data class LiveviewState(
    val iso: Int? = null,
)

@Composable
fun GrayButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    text: String = "",
    content: @Composable () -> Unit = {Text(text, color = Color.White, modifier = Modifier.padding(10.dp))},
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = LightGray.copy(alpha = 0.5f)),
        modifier = modifier,
        shape = RectangleShape,
        enabled = enabled,
    ) {
        content()
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape", uiMode = 32)
@Composable
fun PreviewLiveview2() {
    PreviewLiveview()
}


@Composable
fun LiveviewButton(modifier: Modifier = Modifier, text: String, icon: Int, shortText: String) {
    GrayButton(modifier, onClick = {

    }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(icon),
                tint = Color.White,
                contentDescription = text
            )
            //Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun Liveview(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController(), state: LiveviewState) {
    Box(modifier.fillMaxSize()) {
        @Composable
        fun buttons(modifier: Modifier) {
            LiveviewButton(modifier, text = "White Balance", R.drawable.outline_wb_sunny_24, "WB")
            LiveviewButton(modifier, text = "ISO", R.drawable.outline_iso_24, "ISO")
            LiveviewButton(modifier, text = "Shutter Speed", R.drawable.outline_shutter_speed_24, "SHUTT")
            LiveviewButton(modifier, text = "Aperture", R.drawable.outline_camera_24, "APERT")
            LiveviewButton(modifier, text = "Format", R.drawable.outline_style_24, "FORMAT")
        }
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val modifier = Modifier.weight(1f)
                buttons(modifier)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                val modifier = Modifier.weight(1f)
                buttons(modifier)
            }
        }
        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(R.drawable.image),
            contentDescription = null
        )

        Row(Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(onClick = {

            }) {
                Icon(painterResource(R.drawable.outline_camera_24), contentDescription = null, Modifier.size(200.dp))
            }
        }
    }
}