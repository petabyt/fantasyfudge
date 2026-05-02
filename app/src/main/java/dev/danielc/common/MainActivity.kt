package dev.danielc.common

import android.app.ComponentCaller
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.danielc.R
import dev.danielc.common.screens.AboutScreen
import dev.danielc.common.screens.HelpScreen
import dev.danielc.common.screens.MainScreen
import dev.danielc.common.screens.ModuleInstanceNav
import dev.danielc.common.screens.ModuleListScreen
import dev.danielc.common.screens.PreviewGalleryScreen
import dev.danielc.common.screens.PreviewDashboardCamera
import dev.danielc.common.screens.PreviewViewer
import dev.danielc.fudge.AndroidRuntime
import dev.danielc.libpak.Pak
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        Pak.onPermissionResult(requestCode, permissions, grantResults);
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        Pak.onActivityResult(requestCode, resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidRuntime.setupAndroidContext(this)
        Pak.setupAndroidContext(this)
        if (!AndroidRuntime.hasInited) {
            System.loadLibrary("fudge")
            AndroidRuntime.init()
            val manifests = AndroidRuntime.getJsonManifestList()
            Runtime.loadModulesFromManifests(manifests)
            AndroidRuntime.hasInited = true
        }
        enableEdgeToEdge()

        setContent {
            val duration = 200
            val navController = rememberNavController()
            NavHost(
                enterTransition = {
                    slideIn(
                        initialOffset = { IntOffset(it.width, 0) },
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    slideOut(
                        targetOffset = { IntOffset(-it.width / 4, 0) },
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideIn(
                        initialOffset = { IntOffset(-it.width / 4, 0) },
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOut(
                        targetOffset = { IntOffset(it.width, 0) },
                        animationSpec = tween(duration, easing = FastOutSlowInEasing)
                    )
                },

                navController = navController, startDestination = "home") {
                composable("home") {
                    MainScreen(navController)
                }
                composable("help") {
                    HelpScreen(navController)
                }
                composable("about") {
                    AboutScreen(navController)
                }
                composable("modules-list") {
                    ModuleListScreen(navController)
                }
                composable<SerializableModuleInstance> { backStackEntry ->
                    val inst = backStackEntry.toRoute<SerializableModuleInstance>()
                    val module = inst.getModuleInstance()
                    ModuleInstanceNav(module, backToMainScreen = {
                        navController.popBackStack()
                    })
                }
                composable("gallery") { PreviewGalleryScreen(navController) }
                composable("preview-viewer") { PreviewViewer(navController) }
                composable("test-dashboard1") { PreviewDashboardCamera() }
            }

            if (true) {
                LaunchedEffect(Unit) {
                    val instance = Runtime.createModuleInstance(Runtime.getManifestFromName("libfuji")!!)
                    navController.navigate(instance.serializableModuleInstance)
                    instance.initThread()
                }
            }
        }
    }
}