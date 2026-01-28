package dev.danielc.common

import android.content.res.Configuration
import android.hardware.lights.Light
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dev.danielc.R
import dev.danielc.common.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JobProgressBarModel : ViewModel() {
    private val _uiState = MutableStateFlow(0)
    val uiState: StateFlow<Int> = _uiState.asStateFlow()

    fun update(p: Int) {
        viewModelScope.launch() {
            withContext(Dispatchers.Default) {
                _uiState.value = p
            }
        }
    }
}

object Widgets {
    val pad = 10.dp

    @Composable
    fun longPress(onClick: () -> Unit, onLongClick: () -> Unit): MutableInteractionSource {
        val context = LocalContext.current

        val interactionSource = remember { MutableInteractionSource() }

        val viewConfiguration = LocalViewConfiguration.current

        LaunchedEffect(interactionSource) {
            var isLongClick = false

            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        isLongClick = false
                        delay(viewConfiguration.longPressTimeoutMillis)
                        isLongClick = true
                        onLongClick()
                    }

                    is PressInteraction.Release -> {
                        if (isLongClick.not()) {
                            onClick()
                        }
                    }

                }
            }
        }

        return interactionSource
    }

    @Composable
    fun Button2(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: (Int) -> Unit,
        content: @Composable () -> Unit,
        ctx: Int
    ) {
        Button(
            onClick = {
                onClick(ctx)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Teal700),
            modifier = modifier.padding(pad).fillMaxWidth(),
            shape = RectangleShape,
            enabled = enabled,
        ) {
            content()
        }
    }

    @Composable
    fun GreenButton(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit,
        text: String = "",
        content: @Composable () -> Unit = {Text(text, color = Color.White, modifier = Modifier.padding(pad))},
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
            modifier = modifier,
            shape = RectangleShape,
            enabled = enabled,
        ) {
            content()
        }
    }

    @Composable
    fun BlueButton(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen2),
            modifier = modifier,
            shape = RectangleShape,
            enabled = enabled,
        ) {
            content()
        }
    }

    @Composable
    fun GrayButton(
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        onClick: () -> Unit,
        text: String = "",
        content: @Composable () -> Unit = {Text(text, color = Color.White, modifier = Modifier.padding(pad))},
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = LightGray),
            modifier = modifier,
            shape = RectangleShape,
            enabled = enabled,
        ) {
            content()
        }
    }

    @Composable
    fun Iconbutton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        Button(
            shape = RectangleShape,
            modifier = modifier.size(50.dp),
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(0.dp)
        ) {
            content()
        }
    }

    @Composable
    fun LongClickButton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        text: String = "",
        content: @Composable () -> Unit = {Text(text, color = Color.White, modifier = Modifier.padding(pad))},
    ) {
        val longPress = longPress(onClick = onClick, onLongClick = onLongClick)
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = GoGreen),
            shape = RectangleShape,

            modifier = modifier,
            interactionSource = longPress,
            onClick = {}
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewButton() {
    return Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Widgets.LongClickButton(
            text = "asd",
            onClick = {

            },
            onLongClick = {

            },
        )
        Widgets.GreenButton(
            text = "text",
            onClick = {

            },
        )
        Widgets.Iconbutton(onClick = {

        }) {
            Icon(painter = painterResource(R.drawable.baseline_bug_report_24),
                contentDescription = null)
        }
    }
}