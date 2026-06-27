package dev.danielc.common
import dev.danielc.common.screens.ConnectingRequiredAction
import dev.danielc.common.screens.ConsoleViewModel
import dev.danielc.common.screens.GalleryObjectReference
import dev.danielc.common.screens.GalleryViewModel
import dev.danielc.common.screens.ModuleInstanceModel
import dev.danielc.common.screens.SortBy
import dev.danielc.common.screens.ViewerModel
import dev.danielc.fudge.AndroidRuntime
import dev.danielc.fudge.NativeModule
import dev.danielc.libpak.Bluetooth
import dev.danielc.libpak.Pak
import dev.danielc.libpak.WiFi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

typealias JobUpdateCallback = (ModuleJob) -> Unit

/**
 * A job id is passed each time a module function is called. A job can be canceled
 * at any time by the user, and the percent finished value can be updated by the module.
 */
@Serializable
data class ModuleJob(
    val onUpdate: JobUpdateCallback,
    val id: Int,
    var progressBarValue: Int? = null,
    var isCancelled: Boolean = false,
    var isFinished: Boolean = false,
)

class ModuleGalleryViewModel: GalleryViewModel() {
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

abstract class ModuleBase: NativeModule() {
    private var jobs = mutableMapOf<Int, ModuleJob>()
    private var jobCounter = 0
    fun createJob(onUpdate: JobUpdateCallback): ModuleJob {
        val id = ++jobCounter
        val job = ModuleJob(
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
        var rc = block(job)
        if (job.isCancelled) rc = Pak.Error.CANCELLED
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
    fun runCommand(cmd: String, vararg params: String): Int {
        return withJob({}) { job ->
            val list = listOf(cmd) + params
            onRunCommand(job.id, list.getOrNull(0), list.getOrNull(1), list.getOrNull(2), list.getOrNull(3))
        }
    }
    fun runCommand(cmd: Command): Int {
        return runCommand(cmd.cmd)
    }
    fun tryConnectBluetooth(device: Bluetooth.Device, saved: SavedDeviceEntity?, onUpdate: JobUpdateCallback = {}): Int {
        return withJob(onUpdate) { job ->
            onTryConnectBluetooth(device, if (saved == null) null else SavedDeviceInfo(
                saved.uniqueIdentifier,
                saved.name,
                saved.privateData,
            ), job.id)
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
    val savedDeviceUniqueId: String? = null,
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
    fun getSavedDeviceEntity(): SavedDeviceEntity? {
        return if (savedDeviceUniqueId == null) null else Runtime.findSavedDeviceEntity(savedDeviceUniqueId)
    }
}

/**
 * Instance of a module with a single connection
 */
class ModuleInstance(val manifest: ModuleManifest, val request: ModuleInstanceRequest, val homeModelView: ModuleInstanceModel, viewModels: ViewModelReferences): ModuleBase() {
    val galleryViewModel: ModuleGalleryViewModel = viewModels.galleryViewModel
    val viewerViewModel: ViewerModel = viewModels.viewerViewModel
    var viewerDownloadJob: ModuleJob? = null
    val debugLogModel: ConsoleViewModel = viewModels.debugLogModel
    init {
        Runtime.addModuleInstance(this)
        galleryViewModel.module = this
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
    private var connectedBluetoothMacAddress: String? = null

    fun saveDeviceSignature(info: SavedDeviceInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            AndroidRuntime.getDatabase().deviceDao().saveDevice(SavedDeviceEntity(
                info.uniqueIdentifier,
                name = info.name,
                privateData = info.privateData,
                manifestName = manifest.name,
                targetIndex = request.targetIndex,
                setupOption = request.chosenSetupOption,
                bluetoothMacAddress = connectedBluetoothMacAddress,
            ))
        }
    }
    fun trimMemory() {
        galleryViewModel.trimMemory()
    }
    fun setTickRate(us: Int) {
        currentTickIntervalUs = us
    }
    fun debugLog(s: String) {
        debugLogModel.addLine(s)
    }
    fun getSetupOptionName(): String? {
        return request.chosenSetupOption
    }
    suspend fun deregister() {
        println("Deregistering module")
        galleryViewModel.stop()
        initJob?.cancel()
        initJob?.join()
        mainLoopJob?.cancel()
        mainLoopJob?.join()
        Runtime.removeModuleInstance(this)
        if (!homeModelView.initializationError.value) free()
    }
    fun getMetadata(file: FileHandle): FileMetadata? {
        return galleryViewModel.getMetadata(file)
    }
    fun setProperty(type: String, value: String) {
        homeModelView.setProperty(ModuleProperty.fromId(type)!!, value)
    }
    fun setProperty(type: String, value: Int) {
        homeModelView.setProperty(ModuleProperty.fromId(type)!!, value)
    }
    fun setScreenSupported(id: Int, v: Boolean) {
        homeModelView.setSupportedScreen(Screen.fromId(id)!!, v)
    }
    fun setDashboardPane(pane: DashboardPane) {
        homeModelView.setDashboardPane(pane)
    }
    fun setProgressBar(job: Int, v: Int) {
        val jobObj = getJob(job)
        if (jobObj != null) {
            jobObj.progressBarValue = v
            jobObj.onUpdate(jobObj)
        }
    }
    fun isJobCancelled(job: Int): Boolean {
        val jobObj = getJob(job)
        if (jobObj != null) {
            return jobObj.isCancelled
        }
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
        homeModelView.updateNFiles(nItems)
    }

    fun setIsConnected() {
        isConnected = true
        startMainLoop()
        switchScreen(Screen.DASHBOARD, false)
    }

    private fun wifiConnectRoutine(wifi: ModuleManifest.WiFiDiscovery) {
        val primaryAdapter = WiFi.getPrimaryAdapter()
        debugLog("First trying connection over primary adapter...")
        if (tryConnectWiFi(primaryAdapter, {job -> connectCallback(job)}) == 0) {
            setIsConnected()
            return
        }

        val filter = WiFi.ApFilter()
        filter.ssidPattern = wifi.ssidPattern
        val callback = object : WiFi.WiFiDiscoveryCallback() {
            override fun failed(reason: String, code: Int) {
                debugLog("<error>${reason}")
            }

            override fun found(net: WiFi.Adapter) {
                if (tryConnectWiFi(net) == 0) {
                    setIsConnected()
                }
            }
        }
        WiFi.connectToAccessPoint(filter, callback)
    }

    private fun connectCallback(job: ModuleJob) {
        homeModelView.connectProgress.update {
            job.progressBarValue
        }
    }

    private fun bluetoothConnectRoutine(info: ModuleManifest.BluetoothDiscovery, saved: SavedDeviceEntity?) {
        if (!Bluetooth.checkPermission()) {
            homeModelView.connectRequiredAction.value = ConnectingRequiredAction.ACCEPT_BLUETOOTH_PERMISSION
            Bluetooth.requestConnectPermission()
        } else if (!Bluetooth.isBluetoothEnabled()) {
            homeModelView.connectRequiredAction.value = ConnectingRequiredAction.TURN_ON_BLUETOOTH
            Bluetooth.openEnableBluetoothDialog()
        } else {
            fun doConnect(dev: Bluetooth.Device) {
                debugLog("Connecting to '${dev.name}'...")
                connectedBluetoothMacAddress = dev.address
                val rc = tryConnectBluetooth(dev, saved, {job -> connectCallback(job)})
                if (rc == 0) {
                    setIsConnected()
                } else {
                    disconnectReason = "Failed to connect"
                    disconnectedErrorCode = rc
                    switchScreen(Screen.DISCONNECTED, false)
                }
            }

            if (saved != null && saved.bluetoothMacAddress != null) {
                doConnect(Bluetooth.fromAddress(saved.bluetoothMacAddress))
                return
            }

            val devices = Bluetooth.getBondedDevices(Bluetooth.getDefaultAdapter())
            if (devices != null && info.namePattern != null) {
                for (dev in devices) {
                    val r = Regex(info.namePattern)
                    if (r.matches(dev.name)) {
                        doConnect(dev)
                        return
                    }
                }
            }

            val filter = Bluetooth.BtFilter()
            filter.serviceUuids = info.serviceUuids.toTypedArray()
            filter.isClassic = false
            filter.manufacData = info.mfgData
            val callback = object : Bluetooth.ScanCallback() {
                override fun onFound(device: Bluetooth.Device) {
                    doConnect(device)
                }
                override fun onFailure(reason: String) {
                    debugLog(reason)
                }
            }
            val rc = Bluetooth.pairWithDeviceCompanion(filter, "FudgeDevice1", callback)
            debugLog("Companion pairing dialog result: ${rc}")
        }
    }

    fun tryConnectAgain() {
        if (initJob?.isCompleted == true) {
            initThread()
        }
    }

    fun initThread() {
        val module = this
        initJob = CoroutineScope(Dispatchers.IO).launch {
            val rc = when (manifest.moduleType) {
                ModuleManifest.ModuleType.CMF_NOTHING -> AndroidRuntime.setupCmfNothingAudioModule(module)
                ModuleManifest.ModuleType.QUICKJS -> {
                    val path = module.manifest.scriptPath
                    if (path == null) {
                        debugLog("<error>script path not included")
                        -1
                    } else {
                        var fileContents = AndroidRuntime.readFile(path)
                        if (fileContents == null) {
                            debugLog("Failed to read ${path}")
                            -1
                        } else {
                            fileContents += 0.toByte()
                            AndroidRuntime.setupJavascriptModule(module, fileContents)
                        }
                    }
                }
                ModuleManifest.ModuleType.WEBASSEMBLY -> {
                    debugLog("<error>Wasm not implemented yet")
                    -1
                }
                ModuleManifest.ModuleType.DUMMY_MODULE -> AndroidRuntime.setupDummyNativeModule(module)
                ModuleManifest.ModuleType.LIBFUJI -> AndroidRuntime.setupLibFujiModule(module)
                ModuleManifest.ModuleType.GOVEELIFE -> AndroidRuntime.setupGoveeLifeModule(module)
            }
            if (rc != 0) {
                homeModelView.initializationError.value = true
            } else {
                homeModelView.connectRequiredAction.value = ConnectingRequiredAction.NONE
                val savedDeviceInfo = request.getSavedDeviceEntity()
                val option = request.getSetupOption()
                val transport = option?.transport ?: ModuleManifest.Transport.BLUETOOTH
                if (target.wifiDiscovery != null && transport == ModuleManifest.Transport.WIFI_AP) {
                    wifiConnectRoutine(target.wifiDiscovery)
                } else if (target.bluetoothDiscovery != null && transport == ModuleManifest.Transport.BLUETOOTH) {
                    bluetoothConnectRoutine(target.bluetoothDiscovery, savedDeviceInfo)
                } else {
                    val rc = findConnection({ job -> connectCallback(job) })
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
                delay(currentTickIntervalUs.toLong() / 1000)
            }
        }
    }

    fun forceDisconnect(reason: String, code: Int = 0) {
        if (isConnected) {
            isConnected = false
            CoroutineScope(Dispatchers.IO).launch {
                mainLoopJob?.cancel()
                mainLoopJob?.join()
                println("main job killed")
                withJob({}) { job ->
                    onDisconnect()
                }
                println("disconnect called")
                disconnectReason = reason
                disconnectedErrorCode = code
                homeModelView.goToScreen(Screen.DISCONNECTED)
            }
        }
    }

    fun switchScreen(prev: Screen, new: Screen, callback: JobUpdateCallback): Int {
        val rc = withJob(callback) { job ->
            onSwitchScreen(prev.id, new.id, job.id)
        }
        if (new == Screen.FILE_GALLERY) {
            galleryViewModel.start()
        } else if (prev == Screen.FILE_GALLERY) {
            galleryViewModel.setPaused(true)
        }
        return rc
    }

    fun switchScreen(screen: Screen, isInNavBar: Boolean, callback: JobUpdateCallback = {}): Int {
        if (!isNavigating) {
            isNavigating = true
            if (currentScreen != screen) {
                homeModelView.goToScreen(screen, isInNavBar)
            }
            val rc = switchScreen(currentScreen, screen, callback)
            currentScreen = screen
            isNavigating = false
            return rc
        }
        return 0
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
            val galleryState = galleryViewModel.uiState.value
            viewerViewModel.update(file, galleryState.objects.size)
            viewerViewModel.updateMetadata(galleryViewModel.getMetadata(file))
            viewerViewModel.updateSideBitmaps(galleryViewModel.getThumbnail(file, -1), galleryViewModel.getThumbnail(file, 1))

            fun onCancel() {
                CoroutineScope(Dispatchers.IO).launch {
                    goBack(Screen.FILE_GALLERY, false)
                }
            }

            var rc = switchScreen(Screen.FILE_VIEWER, false, { job ->
                viewerDownloadJob = job
                if (job.isFinished) {
                    viewerDownloadJob = null
                }
                viewerViewModel.updateStats(job.progressBarValue ?: 0, "switching to viewer")
            })
            if (rc == Pak.Error.CANCELLED) {
                onCancel()
            } else {
                rc = getFileContents({ job ->
                    viewerDownloadJob = job
                    if (job.isCancelled) {
                        viewerViewModel.setError("Cancelling...")
                        viewerDownloadJob = null
                    }
                    viewerViewModel.updateStats(job.progressBarValue ?: 0, "download speed")
                }, file)
                if (rc == Pak.Error.CANCELLED) {
                    onCancel()
                } else if (rc != 0) {
                    viewerViewModel.setError("Image load error: ${rc}")
                }
            }
        }
    }
}