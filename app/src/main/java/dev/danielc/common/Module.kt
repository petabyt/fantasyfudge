package dev.danielc.common
import dev.danielc.R
import dev.danielc.common.screens.ConsoleStateModel
import dev.danielc.common.screens.GalleryObject
import dev.danielc.common.screens.GalleryViewModel
import dev.danielc.common.screens.ModuleHomeModel
import dev.danielc.common.screens.SortBy
import dev.danielc.common.screens.ViewerModel
import dev.danielc.libpak.Pak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.collections.remove
import kotlin.inc
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

typealias JobUpdateCallback = (ModuleJob) -> Unit

/**
 * A job id is passed each time a module function is called. A job can be cancelled
 * at any time by the user, and the percent finished value can be updated by the module.
 */
@Serializable
data class ModuleJob(
    val onUpdate: JobUpdateCallback,
    val moduleInstance: SerializableModuleInstance,
    val id: Int,
    var progressBarValue: Int? = null,
    var isCancelled: Boolean = false,
    var isFinished: Boolean = false,
)

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
        CMF_NOTHING,
        LIBFUJI;
        fun getDesc(): String {
            return when (this) {
                QUICKJS -> "Javascript"
                WEBASSEMBLY -> "Webassembly"
                NATIVE -> "Native (statically compiled)"
                DUMMY_MODULE -> "DummyModule (statically compiled)"
                CMF_NOTHING -> "libcmf-nothing (statically compiled)"
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
abstract class ModuleInstance(manifest: ModuleManifest) {
    var debugLogModel = ConsoleStateModel()
    val homeModelView = ModuleHomeModel(manifest, this)
    val galleryViewModel = GalleryViewModel()
    val viewerViewModel = ViewerModel()
    var currentTickIntervalUs: Int = (100 * 1000)
    val serializableModuleInstance: SerializableModuleInstance = SerializableModuleInstance(Runtime.addModuleInstance(this))
    private var mainLoopJob: kotlinx.coroutines.Job? = null
    private var initJob: kotlinx.coroutines.Job? = null
    private var currentScreen: Screen = Screen.CONNECT
    var isConnected = false
    var disconnectReason: String? = null
    var disconnectedErrorCode: Int? = null
    private var jobs = mutableMapOf<Int, ModuleJob>()
    private var jobCounter = 0

    fun createJob(mod: SerializableModuleInstance, onUpdate: JobUpdateCallback): ModuleJob {
        val id = ++jobCounter
        val job = ModuleJob(
            moduleInstance = mod,
            id = id,
            onUpdate = onUpdate
        )
        jobs.put(id, job)
        return job
    }

    fun closeJob(job: ModuleJob) {
        val key = jobs.entries.find { it.value == job }?.key
        if (key != null) {
            jobs.remove(key)
        }
    }

    fun getJob(job: Int): ModuleJob? {
        return jobs.entries.find { it.key == job }?.value
    }

    abstract fun free()
    abstract fun onFindConnection(job: Int): Int
    abstract fun onTryConnectWiFi(a: NativeRuntime.WiFiAdapter, job: Int): Int
    abstract fun onDisconnect(): Int
    abstract fun onIdleTick(usSinceLast: Int): Int
    abstract fun onSwitchScreen(oldScreen: Int, newScreen: Int, job: Int): Int
    abstract fun onRequestFileContents(job: Int, file: FileHandle): Int
    abstract fun onRequestFileThumbnail(job: Int, file: FileHandle): Int
    abstract fun onRequestFileMetadata(job: Int, file: FileHandle): Int

    fun setTickRate(us: Int) {
        currentTickIntervalUs = us
    }
    fun debugLog(s: String) {
        debugLogModel.addLine(s)
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
        val jobObj = getJob(job)
        if (jobObj != null) {
            // Do on separate thread?
            jobObj.progressBarValue = v
            jobObj.onUpdate(jobObj)
        }
        // throw err?
    }
    fun isJobCancelled(job: Int): Boolean {
        val jobObj = getJob(job)
        if (jobObj != null) {
            return jobObj.isCancelled
        }
        // throw err?
        return false
    }
    fun addFileMetadata(file: FileHandle, v: FileMetadata) {
        galleryViewModel.updateObject(file.index, v, null)
        // Update the image viewer state if it doesn't already have the metadata
        val viewerState = viewerViewModel.viewerState.value
        if (viewerState != null) {
            if (viewerState.handle.index == file.index && viewerState.handle.storageName == file.storageName) {
                viewerViewModel.update(file, galleryViewModel.uiState.value.objects.size)
            }
        }
    }
    fun setFileContents(file: FileHandle, data: ByteArray) {
        viewerViewModel.setFileContents(data)
    }
    fun addFileThumbnail(file: FileHandle, data: ByteArray) {
        galleryViewModel.updateObject(file.index, null, data)
    }
    fun setStorageInfo(nItems: Int, name: String, sortBy: Int) {
        // TODO: Manage multiple storage devices
        galleryViewModel.setProperties(nItems, name, SortBy.fromId(sortBy)!!)
        // TODO: Count total files if needed
        homeModelView.dashboardState.value.copy(
            filesOnStorage = nItems
        )
    }
    fun setFileListLength(length: Int) {
        galleryViewModel.setListLength(length)
    }

    fun initThread() {
        initJob = CoroutineScope(Dispatchers.IO).launch {
            if (findConnection() == Pak.Error.OK.code) {
                isConnected = true
                startMainLoop()
                switchScreen(Screen.DASHBOARD, false)
            } else {
                debugLog("Failed to find connection")
                switchScreen(Screen.DISCONNECTED, false)
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
        val job = createJob(serializableModuleInstance, callback)
        val rc = block(job)
        job.progressBarValue = null
        job.isFinished = true
        callback(job)
        closeJob(job)
        return rc
    }

    private fun reportFatalError(code: Int, reason: String) {
        disconnectedErrorCode = code
        disconnect(reason)
    }

    fun disconnect(reason: String) {
        withJob({}) { job ->
            onDisconnect()
        }
        isConnected = false
        disconnectReason = reason
        homeModelView.goToScreen(Screen.DISCONNECTED)
    }

    fun switchScreen(screen: Screen, isInNavBar: Boolean, callback: JobUpdateCallback = {}) {
        withJob(callback) { job ->
            onSwitchScreen(currentScreen.id, screen.id, job.id)
        }
        currentScreen = screen
        homeModelView.goToScreen(screen, isInNavBar)
    }

    fun goBack(previous: Screen, isInNavBar: Boolean, callback: JobUpdateCallback = {}) {
        withJob(callback) { job ->
            onSwitchScreen(currentScreen.id, previous.id, job.id)
        }
        currentScreen = previous
        homeModelView.back(isInNavBar)
    }

    fun findConnection(onUpdate: JobUpdateCallback = {}): Int {
        return withJob(onUpdate) { job ->
            onFindConnection(job.id)
        }
    }

    fun getFileMetadata(onUpdate: JobUpdateCallback = {}, file: FileHandle): Int {
        return withJob(onUpdate) { job ->
            onRequestFileMetadata(job.id, file)
        }
    }

    fun getFileContents(onUpdate: JobUpdateCallback = {}, file: FileHandle): Int {
        return withJob(onUpdate) { job ->
            onRequestFileContents(job.id, file)
        }
    }

    fun cancelJob(job: ModuleJob) {
        job.isCancelled = true
    }
}

// Serializable ID of connection instance that can be passed between activities
@Serializable
data class SerializableModuleInstance(
    val connectionId: Int,
) {
    fun getModuleInstance(): ModuleInstance {
        val mod = Runtime.moduleInstances[connectionId]
        if (mod == null) {
            throw Exception("Couldn't find module instance from key")
        }
        return mod
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

class CmfNothingModule(manifest: ModuleManifest) : NativeModule(manifest) {
    init {
        NativeRuntime.setupCmfNothingAudioModule(this, manifest)
    }
}