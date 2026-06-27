package dev.danielc.fudge

import android.app.ComponentCaller
import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import dev.danielc.common.ModuleInstanceRequest
import dev.danielc.common.Runtime
import dev.danielc.common.screens.MainNav
import dev.danielc.libpak.Pak
import dev.danielc.libpak.WiFi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), ComponentCallbacks2 {
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
        Pak.onActivityResult(requestCode, resultCode, data)
    }

    override fun onTrimMemory(level: Int) {
        super<ComponentActivity>.onTrimMemory(level)
        Log.d("main", "onTrimMemory")
        for (e in Runtime.moduleInstances) {
            e.value.trimMemory()
        }
        CoroutineScope(Dispatchers.Default).launch {
            Runtime.emitMemoryTrimSignal()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Pak.setupAndroidContext(this)
        if (!AndroidRuntime.hasInited) {
            AndroidRuntime.hasInited = true
            System.loadLibrary("fudge")
            AndroidRuntime.setup(this)
            WiFi.startNetworkListeners(this)
            CoroutineScope(Dispatchers.IO).launch {
                Runtime.refreshManifests()
                Runtime.refreshConnectableDevices()
            }
        }
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            MainNav(navController)
            if (false) {
                LaunchedEffect(Unit) {
                    navController.navigate(ModuleInstanceRequest("goveelife", 0))
                }
            }
        }
    }
}