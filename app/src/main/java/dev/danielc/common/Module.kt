package dev.danielc.common
import dev.danielc.R
import dev.danielc.common.screens.ConsoleStateModel
import dev.danielc.common.screens.GalleryObjectReference
import dev.danielc.common.screens.GalleryViewModel
import dev.danielc.common.screens.ModuleInstanceModel
import dev.danielc.common.screens.SortBy
import dev.danielc.common.screens.ViewerModel
import dev.danielc.fudge.AndroidRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
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
            PROFESSIONAL_CAMERA -> R.drawable.baseline_photo_camera_24
            ACTION_CAMERA -> R.drawable.outline_videocam_24
            DASHCAM -> R.drawable.outline_camera_video_24
            GENERIC_CAMERA -> R.drawable.baseline_photo_camera_24
            WIFI_SD_CARD -> R.drawable.outline_sd_card_24
            DOORBELL -> R.drawable.outline_general_device_24
            GENERIC_HOME_DEVICE -> R.drawable.outline_general_device_24
            DESK -> R.drawable.outline_general_device_24
            GENERIC_FURNITURE -> R.drawable.outline_general_device_24
            PRINTER_3D -> R.drawable.outline_general_device_24
            HEADPHONES -> R.drawable.outline_headphones_24
            EARBUDS -> R.drawable.outline_earbuds_2_24
            SPEAKERS -> R.drawable.outline_speaker_24
            GENERIC_AUDIO -> R.drawable.outline_speaker_24
            SMART_GLASSES -> R.drawable.outline_eyeglasses_2_24
            SMART_TV -> R.drawable.outline_connected_tv_24
            SMARTWATCH -> R.drawable.outline_watch_24
            GENERIC_MEDICAL_WEARABLE -> R.drawable.outline_general_device_24
            GENERIC_EXERCISE_MACHINE -> R.drawable.outline_general_device_24
            POWER_TOOL -> R.drawable.outline_tools_power_drill_24
            GAME_CONTROLLER -> R.drawable.outline_videogame_asset_24
            DRONE -> R.drawable.outline_general_device_24
            GENERIC_REMOTE_CONTROL -> R.drawable.outline_general_device_24
            SCOOTER -> R.drawable.outline_general_device_24
            BICYCLE -> R.drawable.outline_general_device_24
            GENERIC_RIDEABLE -> R.drawable.outline_general_device_24
            AUTOMOTIVE_INFOTAINMENT -> R.drawable.outline_directions_car_24
            AUTOMOTIVE_DIAGNOSTIC -> R.drawable.outline_car_repair_24
            GENERIC_AUTOMOTIVE -> R.drawable.outline_directions_car_24
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
    val setupOptions: List<SetupOption> = emptyList(),
    val publicKey: String? = null,
    val isDraft: Boolean = false,
    val moduleType: ModuleType = ModuleType.NATIVE,
    ) {
    enum class ModuleType() {
        QUICKJS,
        WEBASSEMBLY,
        NATIVE,
        DUMMY_MODULE,
        CMF_NOTHING,
        GOVEELIFE,
        LIBFUJI;
        fun getDesc(): String {
            return when (this) {
                QUICKJS -> "Javascript"
                WEBASSEMBLY -> "Webassembly"
                NATIVE -> "Native (statically compiled)"
                DUMMY_MODULE -> "DummyModule (statically compiled)"
                CMF_NOTHING -> "libcmf-nothing (statically compiled)"
                LIBFUJI -> "libfuji (statically compiled)"
                GOVEELIFE -> "..."
            }
        }
    }
    data class SetupOption(
        val name: String,
        val title: String,
    )
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

class ModuleGalleryViewModel(): GalleryViewModel() {
    var module: ModuleInstance? = null
    override fun fulfillThumbnail(file: GalleryObjectReference) {
        module?.getFileThumbnail(file = FileHandle(file.index, null))
    }

    override fun fulfillMetadata(file: GalleryObjectReference) {
        module?.getFileMetadata(file = FileHandle(file.index, null))
    }
}

data class ViewModelReferences(
    val galleryViewModel: ModuleGalleryViewModel,
    val viewerViewModel: ViewerModel,
    val debugLogModel: ConsoleStateModel,
)

/**
 * Instance of a module with a single connection
 */
class ModuleInstance(val manifest: ModuleManifest, val homeModelView: ModuleInstanceModel, viewModels: ViewModelReferences): NativeModule() {
    val galleryViewModel: ModuleGalleryViewModel = viewModels.galleryViewModel
    val viewerViewModel: ViewerModel = viewModels.viewerViewModel
    val debugLogModel: ConsoleStateModel = viewModels.debugLogModel
    val serializableModuleInstance: SerializableModuleInstance
    init {
        galleryViewModel.module = this
        serializableModuleInstance = SerializableModuleInstance(Runtime.addModuleInstance(this))
        when (manifest.moduleType) {
            ModuleManifest.ModuleType.CMF_NOTHING -> AndroidRuntime.setupCmfNothingAudioModule(this, manifest)
            ModuleManifest.ModuleType.QUICKJS -> throw Exception("QuickJS")
            ModuleManifest.ModuleType.WEBASSEMBLY -> throw Exception("Webassembly")
            ModuleManifest.ModuleType.NATIVE -> throw Exception("Native module (?)")
            ModuleManifest.ModuleType.DUMMY_MODULE -> AndroidRuntime.setupDummyNativeModule(this, manifest)
            ModuleManifest.ModuleType.LIBFUJI -> AndroidRuntime.setupLibFujiModule(this, manifest)
            ModuleManifest.ModuleType.GOVEELIFE -> AndroidRuntime.setupGoveeLifeModule(this, manifest)
        }
    }

    var currentTickIntervalUs: Int = (100 * 1000)
    private var mainLoopJob: kotlinx.coroutines.Job? = null
    private var initJob: kotlinx.coroutines.Job? = null
    private var currentScreen: Screen = Screen.CONNECT
    var isConnected = false
    var disconnectReason: String? = null
    var disconnectedErrorCode: Int? = null
    private var jobs = mutableMapOf<Int, ModuleJob>()
    private var jobCounter = 0
    private var isNavigating = false

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

    fun cancelJob(job: ModuleJob) {
        job.isCancelled = true
    }

    fun setTickRate(us: Int) {
        currentTickIntervalUs = us
    }
    fun debugLog(s: String) {
        debugLogModel.addLine(s)
    }
    suspend fun deregister() {
        println("Deregistering module")
        galleryViewModel.stop()
        initJob?.cancel()
        initJob?.join()
        mainLoopJob?.cancel()
        mainLoopJob?.join()
        Runtime.removeModuleInstance(this)
        free()
    }
    fun getMetadata(file: FileHandle): FileMetadata? {
        return galleryViewModel.getMetadata(file)
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
    fun addFileMetadata(file: FileHandle, v: FileMetadata?) {
        galleryViewModel.updateMetadata(file.index, v)
        // Update the image viewer state if it doesn't already have the metadata
        val viewerState = viewerViewModel.viewerState.value
        if (viewerState != null) {
            if (viewerState.handle.index == file.index && viewerState.handle.storageName == file.storageName) {
                viewerViewModel.update(file, galleryViewModel.uiState.value.objects.size)
                viewerViewModel.updateMetadata(v)
            }
        }
    }
    fun setFileContents(file: FileHandle, data: ByteArray, isPartial: Boolean) {
        // TODO: check viewer matches file
        viewerViewModel.setFileContents(data, isPartial)
    }
    fun addFileThumbnail(file: FileHandle, data: ByteArray) {
        galleryViewModel.updateThumbnail(file.index, data)
    }
    fun setStorageInfo(nItems: Int, name: String, sortBy: Int) {
        // TODO: Manage multiple storage devices
        galleryViewModel.setProperties(nItems, name, SortBy.fromId(sortBy)!!)
        // TODO: Count total files if needed
        homeModelView.dashboardState.value.copy(
            filesOnStorage = nItems
        )
    }

    fun initThread() {
        initJob = CoroutineScope(Dispatchers.IO).launch {
            if (manifest.setupOptions.isEmpty()) {
                if (findConnection() == 0) {
                    isConnected = true
                    startMainLoop()
                    switchScreen(Screen.DASHBOARD, false)
                } else {
                    debugLog("Failed to find connection")
                    switchScreen(Screen.DISCONNECTED, false)
                }
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
        if (isConnected) {
            isConnected = false
            withJob({}) { job ->
                onDisconnect()
            }
            disconnectReason = reason
            homeModelView.goToScreen(Screen.DISCONNECTED)
        }
    }

    fun switchScreen(prev: Screen, new: Screen, callback: JobUpdateCallback) {
        withJob(callback) { job ->
            onSwitchScreen(prev.id, new.id, job.id)
        }
        if (new == Screen.FILE_GALLERY) {
            galleryViewModel.setPaused(false)
        } else if (prev == Screen.FILE_GALLERY) {
            galleryViewModel.setPaused(true)
        }
    }

    fun switchScreen(screen: Screen, isInNavBar: Boolean, callback: JobUpdateCallback = {}) {
        if (!isNavigating) {
            isNavigating = true
            if (currentScreen != screen) {
                homeModelView.goToScreen(screen, isInNavBar)
            }
            switchScreen(currentScreen, screen, callback)
            currentScreen = screen
            isNavigating = false
        }
    }

    fun goBack(previous: Screen, isInNavBar: Boolean, callback: JobUpdateCallback = {}) {
        if (!isNavigating) {
            isNavigating = true
            homeModelView.back(isInNavBar)
            switchScreen(currentScreen, previous, callback)
            currentScreen = previous
            isNavigating = false
        }
    }

    fun goToViewer(file: FileHandle) {
        CoroutineScope(Dispatchers.IO).launch {
            //viewerViewModel.clear()

            val galleryState = galleryViewModel.uiState.value
            viewerViewModel.update(file, galleryState.objects.size)
            viewerViewModel.updateMetadata(galleryViewModel.getMetadata(file))
            viewerViewModel.updateSideBitmaps(galleryViewModel.getThumbnail(file, -1), galleryViewModel.getThumbnail(file, 1))

            switchScreen(Screen.FILE_VIEWER, false, { job ->
                viewerViewModel.updateStats(job.progressBarValue ?: 0, "switching to viewer")
            })

            val rc = getFileContents({ job ->
                viewerViewModel.updateStats(job.progressBarValue ?: 0, "speed")
            }, file)
            if (rc != 0) {
                viewerViewModel.setError("Image load error: ${rc}")
            }
        }
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

    fun getFileThumbnail(onUpdate: JobUpdateCallback = {}, file: FileHandle): Int {
        return withJob(onUpdate) { job ->
            onRequestFileThumbnail(job.id, file)
        }
    }

    fun getFileContents(onUpdate: JobUpdateCallback = {}, file: FileHandle): Int {
        return withJob(onUpdate) { job ->
            onRequestFileContents(job.id, file)
        }
    }
}

// Serializable ID of connection instance that can be passed between activities
// TODO: Not used right now
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

// Serializable ID of connection instance that can be passed between activities
@Serializable
data class ModuleInstanceRequest(
    val manifestName: String,
    val productName: String?,
)