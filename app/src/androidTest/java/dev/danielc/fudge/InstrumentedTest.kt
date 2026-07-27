package dev.danielc.fudge

import android.companion.WifiDeviceFilter
import android.content.Context
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.danielc.common.Runtime
import dev.danielc.libpak.Bluetooth
import dev.danielc.libpak.WiFi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstrumentedTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    external fun getTestWiFiApFilter(): WiFi.ApFilter

    @Test
    fun useAppContext() {
        activityRule.scenario.onActivity { ctx ->
            assertEquals("dev.danielc.fantasyfudge", ctx.packageName)
            AndroidRuntime.logGlobalLine("Hello")
            AndroidRuntime.getDeviceFriendlyName()
            AndroidRuntime.getDownloadDirectory()
            AndroidRuntime.getFiles()

            testBluetooth()
            testWiFi(ctx)

            startModule("dummymod", null)
        }
    }

    fun testBluetooth() {
        Bluetooth.getBondedDevices(Bluetooth.getDefaultAdapter())
        Bluetooth.checkPermission()
        Bluetooth.isBluetoothEnabled()
    }

    fun testWiFi(ctx: Context) {
        WiFi.isWiFiModuleCapableOfHandlingTwoConnections(ctx)
        WiFi.isHotSpotEnabled(ctx)
        WiFi.openHotSpotSettings(ctx)

        val filter = getTestWiFiApFilter()
        assertEquals(filter.hidden, true)
        assertEquals(filter.password, "123456789")
        assertEquals(filter.ssidPattern, "Test.*")
    }
}