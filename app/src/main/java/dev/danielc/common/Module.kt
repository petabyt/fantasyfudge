package dev.danielc.common
import dev.danielc.common.screens.ConsoleViewModel
import dev.danielc.common.screens.GalleryObjectReference
import dev.danielc.common.screens.GalleryViewModel
import dev.danielc.common.screens.ModuleInstanceModel
import dev.danielc.common.screens.SortBy
import dev.danielc.common.screens.ViewerModel
import dev.danielc.fudge.AndroidRuntime
import dev.danielc.libpak.Bluetooth
import dev.danielc.libpak.WiFi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
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
    val debugLogModel: ConsoleViewModel,
)

abstract class ModuleBase(): NativeModule() {
    abstract val serializableModuleInstance: SerializableModuleInstance
    private var jobs = mutableMapOf<Int, ModuleJob>()
    private var jobCounter = 0
    fun createJob(onUpdate: JobUpdateCallback): ModuleJob {
        val id = ++jobCounter
        val job = ModuleJob(
            moduleInstance = serializableModuleInstance,
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

    fun withJob(callback: JobUpdateCallback, block: (ModuleJob) -> Int): Int {
        val job = createJob(callback)
        val rc = block(job)
        job.progressBarValue = null
        job.isFinished = true
        callback(job)
        closeJob(job)
        return rc
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

    fun tryConnectWiFi(net: WiFi.Adapter, onUpdate: JobUpdateCallback = {}): Int {
        return withJob(onUpdate) { job ->
            onTryConnectWiFi(net, job.id)
        }
    }
}

/**
 * Serializable ID of connection instance that can be passed between activities
 */
@Serializable
data class ModuleInstanceRequest(
    val manifestName: String,
    val targetIndex: Int,
    val chosenSetupOption: String? = null,
) {
    fun getManifest(): ModuleManifest {
        return Runtime.getManifestFromName(manifestName)!!
    }
    fun getTarget(): ModuleManifest.Target {
        return getManifest().targets[targetIndex]
    }
    fun getSetupOption(): ModuleManifest.SetupOption? {
        for (t in getTarget().setupOptions) {
            if (chosenSetupOption == t.name) {
                return t
            }
        }
        return null
    }
}

/**
 * Instance of a module with a single connection
 */
class ModuleInstance(val manifest: ModuleManifest, val request: ModuleInstanceRequest, val homeModelView: ModuleInstanceModel, viewModels: ViewModelReferences): ModuleBase() {
    override val serializableModuleInstance = SerializableModuleInstance(Runtime.addModuleInstance(this))
    val galleryViewModel: ModuleGalleryViewModel = viewModels.galleryViewModel
    val viewerViewModel: ViewerModel = viewModels.viewerViewModel
    val debugLogModel: ConsoleViewModel = viewModels.debugLogModel
    init {
        galleryViewModel.module = this
        when (manifest.moduleType) {
            ModuleManifest.ModuleType.CMF_NOTHING -> AndroidRuntime.setupCmfNothingAudioModule(this, manifest)
            ModuleManifest.ModuleType.QUICKJS -> throw Exception("QuickJS")
            ModuleManifest.ModuleType.WEBASSEMBLY -> throw Exception("Webassembly")
            ModuleManifest.ModuleType.DUMMY_MODULE -> AndroidRuntime.setupDummyNativeModule(this, manifest)
            ModuleManifest.ModuleType.LIBFUJI -> AndroidRuntime.setupLibFujiModule(this, manifest)
            ModuleManifest.ModuleType.GOVEELIFE -> AndroidRuntime.setupGoveeLifeModule(this, manifest)
        }
    }

    val target = manifest.targets[request.targetIndex]
    var currentTickIntervalUs: Int = (100 * 1000)
    private var mainLoopJob: kotlinx.coroutines.Job? = null
    private var initJob: kotlinx.coroutines.Job? = null
    private var currentScreen: Screen = Screen.CONNECT
    var isConnected = false
    var disconnectReason: String? = null
    var disconnectedErrorCode: Int? = null
    private var isNavigating = false

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

    fun setIsConnected() {
        isConnected = true
        startMainLoop()
        switchScreen(Screen.DASHBOARD, false)
    }

    fun wifiConnectRoutine(wifi: ModuleManifest.WiFiDiscovery) {
        val primaryAdapter = WiFi.getPrimaryAdapter()
        if (tryConnectWiFi(primaryAdapter) == 0) {
            setIsConnected()
            return
        }

        val filter = WiFi.ApFilter()
        filter.ssidPattern = wifi.ssidPattern
        val callback = object : WiFi.WiFiDiscoveryCallback() {
            override fun failed(reason: String?, code: Int) {
                debugLog(reason!!)
            }

            override fun found(net: WiFi.Adapter?) {
                if (tryConnectWiFi(net!!) == 0) {
                    setIsConnected()
                }
            }
        }
        WiFi.connectToAccessPoint(filter, callback)
    }

    fun tryConnectAgain() {
        if (initJob?.isCompleted == true) {
            initThread()
        }
    }

    fun initThread() {
        initJob = CoroutineScope(Dispatchers.IO).launch {
            val option = request.getSetupOption()
            val transport = option?.transport ?: ModuleManifest.Transport.BLE
            if (target.wifiDiscovery != null && transport == ModuleManifest.Transport.WIFI_AP) {
                wifiConnectRoutine(target.wifiDiscovery)
            } else if (target.bleDiscovery != null && transport == ModuleManifest.Transport.BLE) {
                val filter = Bluetooth.BtFilter()
                filter.serviceUuids = target.bleDiscovery.serviceUuids.toTypedArray()
                filter.isClassic = false
                filter.manufacData = target.bleDiscovery.mfgData
                val rc = Bluetooth.pairWithDeviceCompanion(filter, "FudgeDevice1")
                debugLog("Return code: ${rc}")
            } else {
                val rc = findConnection({ job ->
                    homeModelView.connectProgress.update {
                        job.progressBarValue
                    }
                })
                if (rc == 0) {
                    setIsConnected()
                } else {
                    disconnectReason = "Failed to connect"
                    disconnectedErrorCode = rc
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
                    forceDisconnect( "onIdleTick", rc)
                    break
                }
                curr = TimeSource.Monotonic.markNow()
                Thread.sleep(currentTickIntervalUs.toLong() / 1000)
            }
        }
    }

    fun forceDisconnect(reason: String, code: Int = 0) {
        if (isConnected) {
            isConnected = false
            withJob({}) { job ->
                onDisconnect()
            }
            disconnectReason = reason
            disconnectedErrorCode = code
            homeModelView.goToScreen(Screen.DISCONNECTED)
        }
    }

    fun switchScreen(prev: Screen, new: Screen, callback: JobUpdateCallback) {
        withJob(callback) { job ->
            onSwitchScreen(prev.id, new.id, job.id)
        }
        if (new == Screen.FILE_GALLERY) {
            galleryViewModel.start()
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
}

// Serializable ID of connection instance
@Serializable
data class SerializableModuleInstance(
    val connectionId: Int
) {
    fun getModuleInstance(): ModuleInstance {
        val mod = Runtime.moduleInstances[connectionId]
        if (mod == null) {
            throw Exception("Couldn't find module instance from key")
        }
        return mod
    }
}