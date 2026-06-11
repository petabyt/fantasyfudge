package dev.danielc.common.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DisconnectDialog(nameOfDevice: String = "FooBar", yes: () -> Unit = {}, no: () -> Unit = {}) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(text = "Disconnect")
        },
        text = {
            Text(text = "Disconnect from ${nameOfDevice}?")
        },
        onDismissRequest = {
            no()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    yes()
                }
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    no()
                }
            ) {
                Text("No")
            }
        }
    )
}

//@Preview(showBackground = true, device = "id:pixel_7", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PermissionDialog(proceed: () -> Unit = {}, reject: () -> Unit = {}) {
    Dialog(onDismissRequest = {
        reject()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
                Text(
                    text = "Android permission needed",
                    modifier = Modifier,
                    style = TextStyle(
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Required to connect to bluetooth devices",
                    modifier = Modifier,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = {
                    proceed()
                }) {
                    Text("Grant")
                }
            }
        }
    }
}

//@Preview
@Composable
private fun PreviewGraph() {
    Column(Modifier.size(200.dp)) {
        IntGridGraph(listOf(0 to 10, 1 to 4, 2 to 4, 3 to 5, 4 to 4, 5 to 7, 6 to 40, 10 to 4, 20 to 40, 40 to 30))
    }
}

@Composable
fun IntGridGraph(
    points: List<Pair<Int, Int>>,
    modifier: Modifier = Modifier
) {
    val dataLineColor = Color(0xffab3e4e)
    val backgroundLineColor = Color(0xff2b2b2b)
    Canvas(modifier = modifier
        .fillMaxSize()
        .background(backgroundLineColor)
        .padding(24.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        var maxX = 0
        var maxY = 0
        for (p in points) {
            if (p.first > maxX) maxX = p.first
            if (p.second > maxY) maxY = p.second
        }

        val spacingX = canvasWidth / maxX
        val spacingY = canvasHeight / maxY

        // Map Pair<Int, Int> to Screen Offset (Float)
        // We flip the Y coordinate: canvasHeight - (y * spacing)
        val pixelPoints = points.map { (x, y) ->
            Offset(
                x = x.toFloat() * spacingX,
                y = canvasHeight - (y.toFloat() * spacingY)
            )
        }

        // Draw the Path connecting the points
        if (pixelPoints.isNotEmpty()) {
            val path = Path().apply {
                val first = pixelPoints.first()
                moveTo(first.x, first.y)
                pixelPoints.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = dataLineColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

private const val transitionDuration = 200

fun NavGraphBuilder.composableSlideBackwards(
    route: String,
    content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)
): Unit {
    composable("disconnected",
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)) },
        content = content
    )
}

@Composable
fun DefaultNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): Unit {
    NavHost(navController, startDestination = startDestination, modifier = modifier, route = route, builder = builder,
        enterTransition = {
            slideIn(
                initialOffset = { IntOffset(it.width, 0) },
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOut(
                targetOffset = { IntOffset(-it.width / 4, 0) },
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideIn(
                initialOffset = { IntOffset(-it.width / 4, 0) },
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOut(
                targetOffset = { IntOffset(it.width, 0) },
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            )
        },

    )
}