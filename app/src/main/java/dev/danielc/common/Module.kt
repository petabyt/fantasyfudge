package dev.danielc.common
import dev.danielc.R
import dev.danielc.common.screens.ConsoleStateModel
import dev.danielc.common.screens.GalleryObject
import dev.danielc.common.screens.GalleryViewModel
import dev.danielc.common.screens.HomeViewModel
import dev.danielc.libpak.Pak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

enum class Device(val id: String) {
    // Photo class
    PROFESSIONAL_CAMERA("professional-camera"),
    ACTION_CAMERA("action-camera"),
    DASHCAM("dashcam"),
    GENERIC_CAMERA("generic-camera"),
    WIFI_SD_CARD("wifi-sd-card"),
    DOORBELL("doorbell"),

    // Home class
    GENERIC_HOME_DEVICE("generic-home-device"),
    DESK("desk"),
    GENERIC_FURNITURE("generic-furniture"),
    PRINTER_3D("3d-printer"),

    // Accessory class
    HEADPHONES("headphones"),
    EARBUDS("earbuds"),
    SPEAKERS("speakers"),
    GENERIC_AUDIO("generic-audio"),
    SMART_GLASSES("smart-glasses"),
    SMART_TV("smart-tv"),
    SMARTWATCH("smartwatch"),
    GENERIC_MEDICAL_WEARABLE("generic-medical-wearable"),
    GENERIC_EXERCISE_MACHINE("generic-exercise-machine"),

    // Non-photo gadget class
    POWER_TOOL("power-tool"),
    GAME_CONTROLLER("game-controller"),
    DRONE("drone"),
    GENERIC_REMOTE_CONTROL("generic-remote-control"),
    SCOOTER("scooter"),
    BICYCLE("bicycle"),
    GENERIC_RIDEABLE("generic-rideable"),
    AUTOMOTIVE_INFOTAINMENT("automotive-infotainment"),
    AUTOMOTIVE_DIAGNOSTIC("automotive-diagnostic"),
    GENERIC_AUTOMOTIVE("generic-automotive");

    companion object {
        fun fromId(id: String?): Device? {
            return entries.find { it.id == id }
        }
    }

    fun getIcon(): Int {
        return when (this) {
            Device.PROFESSIONAL_CAMERA -> R.drawable.baseline_photo_camera_24
            Device.ACTION_CAMERA -> R.drawable.outline_videocam_24
            Device.DASHCAM -> R.drawable.outline_camera_video_24
            Device.GENERIC_CAMERA -> R.drawable.baseline_photo_camera_24
            Device.WIFI_SD_CARD -> R.drawable.outline_sd_card_24
            Device.DOORBELL -> R.drawable.outline_general_device_24
            Device.GENERIC_HOME_DEVICE -> R.drawable.outline_general_device_24
            Device.DESK -> R.drawable.outline_general_device_24
            Device.GENERIC_FURNITURE -> R.drawable.outline_general_device_24
            Device.PRINTER_3D -> R.drawable.outline_general_device_24
            Device.HEADPHONES -> R.drawable.outline_headphones_24
            Device.EARBUDS -> R.drawable.outline_earbuds_2_24
            Device.SPEAKERS -> R.drawable.outline_speaker_24
            Device.GENERIC_AUDIO -> R.drawable.outline_speaker_24
            Device.SMART_GLASSES -> R.drawable.outline_eyeglasses_2_24
            Device.SMART_TV -> R.drawable.outline_connected_tv_24
            Device.SMARTWATCH -> R.drawable.outline_watch_24
            Device.GENERIC_MEDICAL_WEARABLE -> R.drawable.outline_general_device_24
            Device.GENERIC_EXERCISE_MACHINE -> R.drawable.outline_general_device_24
            Device.POWER_TOOL -> R.drawable.outline_tools_power_drill_24
            Device.GAME_CONTROLLER -> R.drawable.outline_videogame_asset_24
            Device.DRONE -> R.drawable.outline_general_device_24
            Device.GENERIC_REMOTE_CONTROL -> R.drawable.outline_general_device_24
            Device.SCOOTER -> R.drawable.outline_general_device_24
            Device.BICYCLE -> R.drawable.outline_general_device_24
            Device.GENERIC_RIDEABLE -> R.drawable.outline_general_device_24
            Device.AUTOMOTIVE_INFOTAINMENT -> R.drawable.outline_directions_car_24
            Device.AUTOMOTIVE_DIAGNOSTIC -> R.drawable.outline_car_repair_24
            Device.GENERIC_AUTOMOTIVE -> R.drawable.outline_directions_car_24
        }
    }
}

/**
 * Constructed manually or loaded from json in modules folder.
 *
 * This may have 'discovery info', which can be used to dynamically match
 * available devices (advertisements, paired devices) to modules without having
 * to initialize a module instance and use a lot of ram
 */
data class ModuleManifest(
    val name: String,
    val description: String? = null,
    val author: String = "Daniel Cook",
    val authorUrl: String? = null,
    val version: Int = 0,
    val requiredRuntimeVersion: Int? = null,
    val runtimeVersion: Int = 0,
    val scriptPath: String? = null,
    val targets: List<Target> = emptyList(),
    val publicKey: String? = null,
    val isDraft: Boolean = false,
    val moduleType: ModuleType = ModuleType.NATIVE,
    ) {
    enum class ModuleType() {
        QUICKJS,
        WEBASSEMBLY,
        NATIVE,
        DUMMY_MODULE,
        JAVA_MODULE,
        LIBFUJI;
        fun getDesc(): String {
            return when (this) {
                QUICKJS -> "Javascript"
                WEBASSEMBLY -> "Webassembly"
                NATIVE -> "Native (statically compiled)"
                DUMMY_MODULE -> "DummyModule"
                JAVA_MODULE -> "JavaDummyModule"
                LIBFUJI -> "libfuji (statically compiled)"
            }
        }
    }
    data class Target(
        val deviceId: Device = Device.PROFESSIONAL_CAMERA,
        val company: String = "",
        val products: List<String> = emptyList(),
    )
    data class WiFiDiscovery(
        val ssidPattern: String,
    )
    @Suppress("ArrayInDataClass")
    data class BleDiscovery(
        val mfgData: ByteArray,
        val mfgDataMask: ByteArray? = null,
        val serviceUuids: List<String>,
    )

    data class UsbDiscovery(
        val pid: Int? = null,
        val vid: Int? = null,
        val usbClass: Int? = null,
    )

    data class RememberedDevice(
        val uniqueId: String,
        val name: String,
    )
}

/**
 * Instance of a module with a single connection
 */
abstract class ModuleInstance(mod: ModuleManifest) {
    val manifest: ModuleManifest = mod
    var debugLog = ConsoleStateModel()
    val homeModelView = HomeViewModel(mod)
    val galleryViewModel = GalleryViewModel()
    var currentTickIntervalUs: Int = 100000
    val serializableModuleInstance: SerializableModuleInstance = SerializableModuleInstance(Runtime.addModuleInstance(this))
    var mainLoopJob: kotlinx.coroutines.Job? = null
    var initJob: kotlinx.coroutines.Job? = null
    var currentScreen: Screen = Screen.NONE

    abstract fun free()
    abstract fun onFindConnection(job: Int): Int
    abstract fun onTryConnectWiFi(a: NativeRuntime.WiFiAdapter, job: Int): Int
    abstract fun onIdleTick(usSinceLast: Int): Int
    abstract fun onSwitchScreen(job: Int, oldScreen: Int, newScreen: Int): Int

    fun debugLog(s: String) {
        debugLog.addLine(s)
    }
    suspend fun deregister() {
        println("Deregistering module")
        initJob?.cancel()
        initJob?.join()
        mainLoopJob?.cancel()
        mainLoopJob?.join()
        Runtime.removeModuleInstance(this)
        free()
    }
    fun disconnect(reason: String) {
        homeModelView.goToScreen(Screen.DISCONNECTED)
    }
    fun setProperty(type: ModuleProperty, value: String) {
        homeModelView.setProperty(type, value)
    }
    fun setProperty(type: String, value: String) {
        homeModelView.setProperty(ModuleProperty.fromId(type)!!, value)
    }
    fun setScreenSupported(id: Int, v: Boolean) {
        homeModelView.setSupportedScreen(Screen.fromId(id)!!, v)
    }
    fun addUserSetting(pane: UserSetting) {
        homeModelView.addSettingPane(pane)
    }
    fun setProgressBar(job: Int, v: Int) {
        // ...
    }
    fun isJobCancelled(job: Int): Boolean {
        return false
    }
    fun addFileMetadata(i: Int, v: GalleryObject) {
        galleryViewModel.setObject(i, v)
    }
    fun setFileListLength(length: Int) {
        galleryViewModel.setListLength(length)
    }

    fun initThread() {
        initJob = CoroutineScope(Dispatchers.IO).launch {
            if (findConnection() == Pak.Error.OK.code) {
                startMainLoop()
                withJob({}) { job ->
                    onSwitchScreen(job.id, Screen.NONE.id, Screen.DASHBOARD.id)
                }
                homeModelView.goToScreen(Screen.DASHBOARD)
            } else {
                debugLog("Failed to find connection")
                homeModelView.goToScreen(Screen.DISCONNECTED)
            }
        }
    }

    fun startMainLoop() {
        val module = this
        mainLoopJob = CoroutineScope(Dispatchers.IO).launch {
            val job = mainLoopJob
            var curr = TimeSource.Monotonic.markNow()
            while (job != null && !job.isCancelled) {
                val rc = module.onIdleTick(curr.elapsedNow().toInt(DurationUnit.MICROSECONDS))
                if (rc != 0) {
                    module.reportFatalError(rc, "onIdleTick")
                    break
                }
                curr = TimeSource.Monotonic.markNow()
                Thread.sleep(currentTickIntervalUs.toLong() / 1000)
            }
        }
    }

    private fun withJob(callback: JobUpdateCallback, block: (ModuleJob) -> Int): Int {
        val job = Runtime.createJob(serializableModuleInstance, callback)
        val rc = block(job)
        Runtime.closeJob(job)
        return rc
    }

    private fun reportFatalError(code: Int, message: String) {

    }

    fun switchScreen(screen: Screen) {
        withJob({}) { job ->
            onSwitchScreen(job.id, currentScreen.id, screen.id)
        }
        currentScreen = screen
        homeModelView.goToScreen(Screen.DASHBOARD)
    }

    fun findConnection(onUpdate: JobUpdateCallback = {}): Int {
        return withJob(onUpdate) { job ->
            onFindConnection(job.id)
        }
    }

    fun cancelJob(job: ModuleJob) {
        job.isCancelled = true
    }
}

// Serializable ID of connection instance that can be passed between activities
@Serializable
data class SerializableModuleInstance(
    val connectionId: Int?,
) {
    fun getModuleInstance(): ModuleInstance {
        if (connectionId == null) {
            throw Exception();
        } else {
            return Runtime.moduleInstances[connectionId]
        }
    }
}

class DummyModule(manifest: ModuleManifest) : NativeModule(manifest) {
    init {
        NativeRuntime.setupDummyNativeModule(this, manifest)
    }
}

class LibFujiModule(manifest: ModuleManifest) : NativeModule(manifest) {
    init {
        NativeRuntime.setupLibFujiModule(this, manifest)
    }
}