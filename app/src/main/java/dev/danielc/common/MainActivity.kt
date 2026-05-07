package dev.danielc.common

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.screens.MainNav
import dev.danielc.fudge.AndroidRuntime
import dev.danielc.libpak.Pak

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
            val navController = rememberNavController()
            MainNav(navController)
            if (false) {
                LaunchedEffect(Unit) {
                    val instance = Runtime.createModuleInstance(Runtime.getManifestFromName("goveelife")!!)
                    navController.navigate(instance.serializableModuleInstance)
                    instance.initThread()
                }
            }
        }
    }
}