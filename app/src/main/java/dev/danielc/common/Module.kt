package dev.danielc.common
import dev.danielc.common.screens.ConnectingRequiredAction
import dev.danielc.common.screens.ConsoleModel
import dev.danielc.common.screens.DashboardModel
import dev.danielc.common.screens.GalleryObjectReference
import dev.danielc.common.screens.GalleryViewModel
import dev.danielc.common.screens.ModuleInstanceModel
import dev.danielc.common.screens.ModuleIntervalometerModel
import dev.danielc.common.screens.SortBy
import dev.danielc.common.screens.ViewerModel
import dev.danielc.fudge.AndroidRuntime
import dev.danielc.fudge.FileLayer
import dev.danielc.fudge.NativeModule
import dev.danielc.libpak.Bluetooth
import dev.danielc.libpak.Pak
import dev.danielc.libpak.WiFi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

annotation class CalledFromNative

typealias JobUpdateCallback = (ModuleJob) -> Unit

/**
 * A job id is passed each time a module function is called. A job can be canceled
 * at any time by the user, and the percent finished value can be updated by the module.
 */
data class ModuleJob(
    val onUpdate: JobUpdateCallback,
    val id: Int,
    var progressBarValue: Int? = null,
    var downloadSpeedString: String? = null,
    var isCancelled: Boolean = false,
    var isFinished: Boolean = false,
)

class ModuleGalleryViewModel(val module: ModuleInstance, val viewerViewModel: ViewerModel): GalleryViewModel() {
    override fun fulfillThumbnail(file: GalleryObjectReference) {
        if (module.getFileThumbnail(file = FileHandle(file.index, null)) != 0) {
            updateThumbnail(file.index, thumbData = null)
        }
    }

    override fun fulfillMetadata(file: GalleryObjectReference) {
        if (module.getFileMetadata(file = FileHandle(file.index, null)) != 0) {
            updateMetadata(file.index, null)
        }
    }

    fun goToViewer(file: FileHandle) {
        CoroutineScope(Dispatchers.IO).launch {
            val galleryState = uiState.value
            viewerViewModel.clear()
            viewerViewModel.update(file, galleryState.objects.size)
            viewerViewModel.updateMetadata(getMetadata(file))
            viewerViewModel.updateThumbnails(getThumbnail(file, -1), getThumbnail(file, 0), getThumbnail(file, 1))

            fun onCancel() {
                CoroutineScope(Dispatchers.IO).launch {
                    module.goBack(Screen.FILE_GALLERY, false)
                }
            }

            viewerViewModel.updateStatus("Switching to viewer...")
            var rc = module.switchScreen(Screen.FILE_VIEWER, false, { job ->
                module.viewerDownloadJob = job
                if (job.isFinished) {
                    module.viewerDownloadJob = null
                }
                viewerViewModel.updateProgress(job.progressBarValue ?: 0)
            })
            viewerViewModel.updateStatus(null)

            if (rc == Pak.Error.CANCELLED) {
                onCancel()
            } else {
                if (getMetadata(file) == null) {
                    viewerViewModel.updateStatus("Grabbing metadata...")
                    module.getFileMetadata(file = file)
                }
                viewerViewModel.updateStatus("Downloading...")
                rc = module.getFileContents({ job ->
                    module.viewerDownloadJob = job
                    if (job.isCancelled) {
                        viewerViewModel.updateStatus("Cancelling...")
                        module.viewerDownloadJob = null
                    }
                    viewerViewModel.updateProgress(job.progressBarValue ?: 0)
                }, file)
                if (rc == Pak.Error.CANCELLED) {
                    onCancel()
                } else if (rc != 0) {
                    viewerViewModel.setError("Image download error: ${rc}")
                } else if (viewerViewModel.viewerState.value?.isLoading == true) {
                    viewerViewModel.setError("BUG: Image not loaded entirely by module")
                }
            }
        }
    }
}

class ModuleDashboardModel(val module: ModuleInstance) : DashboardModel(module.manifest) {
    override fun disconnect() {
        scope.launch {
            module.homeModelView.showDisconnectDialog(true)
        }
    }
    override fun propChanged(pane: DashboardPane) {
        module.propChanged(pane)
    }
    override fun runCommand(line: String) {
        module.runCommand(line)
    }

    override fun save() {
        module.userSave()
    }
}

abstract class ModuleBase: NativeModule() {
    private var jobs = mutableMapOf<Int, ModuleJob>()
    private var jobCounter = 0

    fun cancelAllJobs() {
        for (job in jobs) {
            job.value.isCancelled = true
        }
    }

    fun createJob(onUpdate: JobUpdateCallback): ModuleJob {
        synchronized(jobs) {
            val id = ++jobCounter
            val job = ModuleJob(
                id = id,
                onUpdate = onUpdate
            )
            jobs.put(id, job)
            return job
        }
    }

    fun closeJob(job: ModuleJob) {
        synchronized(jobs) {
            val key = jobs.entries.find { it.value == job }?.key
            if (key != null) {
                jobs.remove(key)
            }
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
    fun propChanged(pane: DashboardPane): Int {
        return withJob({}) { job ->
            onPropChanged(job.id, pane)
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
    val deviceMacAddress: String? = null,
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
class ModuleInstance(val manifest: ModuleManifest, var request: ModuleInstanceRequest, val homeModelView: ModuleInstanceModel): ModuleBase() {
    val viewerViewModel = ViewerModel()
    val galleryViewModel = ModuleGalleryViewModel(this, viewerViewModel)
    val intervalometerModel = ModuleIntervalometerModel(this)
    val debugLogModel = ConsoleModel()
    val dashboardModel = ModuleDashboardModel(this)
    val target = manifest.targets[request.targetIndex]
    var viewerDownloadJob: ModuleJob? = null
    val companionName = "${target.company} ${target.deviceId.getReadableName()}"
    init {
        Runtime.addModuleInstance(this)
        if (request.savedDeviceUniqueId != null) {
            val savedEntry = Runtime.savedDevices.value.find { it.uniqueIdentifier == request.savedDeviceUniqueId }
            if (savedEntry != null) dashboardModel.setSaved()
            // TODO: Set saved/update timestamp
        }
    }
    var currentTickIntervalUs: Int = (100 * 1000)
    private var mainLoopJob: kotlinx.coroutines.Job? = null
    private var initJob: kotlinx.coroutines.Job? = null
    private var currentScreen: Screen = Screen.CONNECT
    var isConnected = false
    var disconnectReason: String? = null
    var disconnectedErrorCode: Int? = null
    private var isNavigating = false

    fun trimMemory() {
        galleryViewModel.onTrimMemory()
    }

    fun userSave() {
        val name = dashboardModel.state.value.nameOfDevice ?: return
        saveDeviceSignature(SavedDeviceInfo(
            uniqueIdentifier = name,
            name = name,
            privateData = null,
        ))
    }

    @CalledFromNative
    fun saveDeviceSignature(info: SavedDeviceInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            dashboardModel.setSaved()
            AndroidRuntime.getDatabase().deviceDao().saveDevice(SavedDeviceEntity(
                uniqueIdentifier = info.uniqueIdentifier,
                name = info.name,
                privateData = info.privateData,
                manifestName = manifest.name,
                targetIndex = request.targetIndex,
                setupOption = request.chosenSetupOption,
                bluetoothMacAddress = getConnectedMacAddress(),
                associationId = takeAndroidAssocationId(),
            ))
        }
    }
    @CalledFromNative
    fun setTickRate(us: Int) {
        currentTickIntervalUs = us
    }
    @CalledFromNative
    fun debugLog(s: String) {
        println(s)
        debugLogModel.addLine(s)
    }
    suspend fun stopAllThreads() {
        println("Stopping module threads")
        cancelAllJobs()
        Bluetooth.interruptAll()
        galleryViewModel.stop()
        initJob?.cancelAndJoin()
        mainLoopJob?.cancelAndJoin()
    }
    suspend fun deregister() {
        stopAllThreads()
        println("Deregistering module")
        Runtime.removeModuleInstance(this)
        if (!homeModelView.initializationError.value) {
            close()
            free()
        }
    }
    @CalledFromNative
    fun getMetadata(file: FileHandle): FileMetadata? {
        return galleryViewModel.getMetadata(file)
    }
    @CalledFromNative
    fun setProperty(type: String, value: String) {
        dashboardModel.setProperty(ModuleProperty.fromId(type) ?: return, value)
    }
    @CalledFromNative
    fun setProperty(type: String, value: Int) {
        dashboardModel.setProperty(ModuleProperty.fromId(type) ?: return, value)
    }
    @CalledFromNative
    fun setScreenSupported(id: Int, v: Boolean) {
        homeModelView.setSupportedScreen(Screen.fromId(id)!!, v)
    }
    @CalledFromNative
    fun setDashboardPane(pane: DashboardPane) {
        dashboardModel.setDashboardPane(pane)
    }
    @CalledFromNative
    fun setProgressBar(job: Int, v: Int) {
        val jobObj = getJob(job)
        if (jobObj != null) {
            jobObj.progressBarValue = v
            jobObj.onUpdate(jobObj)
        }
    }
    fun setDownloadStats(job: Int, timeMs: Long, nBytes: Int) {
        viewerViewModel.updateSpeed("${(nBytes * 8) / timeMs}Mbps")
    }
    @CalledFromNative
    fun isJobCancelled(job: Int): Boolean {
        val jobObj = getJob(job)
        if (jobObj != null) {
            return jobObj.isCancelled
        }
        return false
    }
    @CalledFromNative
    fun addFileMetadata(file: FileHandle, v: FileMetadata?) {
        galleryViewModel.updateMetadata(file.index, v)
        // Update the image viewer state if it doesn't already have the metadata
        val viewerState = viewerViewModel.viewerState.value
        if (viewerState != null) {
            if (viewerState.handle.index == file.index && viewerState.handle.storageName == file.storageName) {
                viewerViewModel.updateMetadata(v)
            }
        }
    }
    private var elapsed = TimeSource.Monotonic.markNow()
    private var isNotUpdatingDownloadSpeed: Boolean = false
    @CalledFromNative
    fun setFileContents(file: FileHandle, data: ByteArray?, offset: Long, totalSize: Long) {
        if ((offset == 0L || isNotUpdatingDownloadSpeed) && data != null) {
            if (!isNotUpdatingDownloadSpeed) if (viewerViewModel.viewerState.value?.currentDownloadSpeed == null) isNotUpdatingDownloadSpeed = true
            var ms = elapsed.elapsedNow().toInt(DurationUnit.MILLISECONDS)
            if (ms == 0) ms++
            viewerViewModel.updateSpeed("${(data.size / ms) / 1000.0}MB/s")
            elapsed = TimeSource.Monotonic.markNow()
        }
        viewerViewModel.setFileContents(data, offset, totalSize)
    }
    @CalledFromNative
    fun addFileThumbnail(file: FileHandle, data: ByteArray) {
        galleryViewModel.updateThumbnail(file.index, data)
    }
    @CalledFromNative
    fun setStorageInfo(nItems: Int, name: String, sortBy: Int) {
        // TODO: Manage multiple storage devices
        galleryViewModel.setProperties(nItems, name, SortBy.fromId(sortBy)!!)
        // TODO: Count total files if needed
        dashboardModel.updateNumFiles(nItems)
    }
    @CalledFromNative
    private fun setIsConnected() {
        isConnected = true
        startMainLoop()
        switchScreen(Screen.DASHBOARD, false)
    }
    @CalledFromNative
    fun addWiFiConnection(filter: WiFi.ApFilter, setupOption: String) {
        CoroutineScope(Dispatchers.IO).launch {
            homeModelView.goToScreen(Screen.CONNECT_SECONDARY, false)
            setSetupOptionName(setupOption)
            val callback = object : WiFi.WiFiDiscoveryCallback() {
                override fun failed(reason: String, code: Int) {
                    debugLog("<error>${reason}")
                }

                override fun onConnected(net: WiFi.Adapter) {
                    debugLog("Found network")
                    request = request.copy(
                        chosenSetupOption = setupOption
                    )
                    val rc = tryConnectWiFi(net)
                    if (rc == 0) {
                        homeModelView.back(false)
                    } else {
                        debugLog("Failed to connect: ${rc}")
                    }
                }
            }
            WiFi.connectToAccessPointCompanion(filter, "", callback, true)
        }
    }

    private fun wifiConnectRoutine(wifi: ModuleManifest.WiFiDiscovery, saved: SavedDeviceEntity?) {
        val callback = object : WiFi.WiFiDiscoveryCallback() {
            override fun failed(reason: String, code: Int) {
                debugLog("<error>${reason}")
            }

            override fun onConnected(net: WiFi.Adapter) {
                if (tryConnectWiFi(net) == 0) {
                    setWiFiDevice(net)
                    setIsConnected()
                }
            }

            override fun onConnecting(ssid: String?) {
                if (ssid != null) debugLog("Connecting to ${ssid}...") else debugLog("Connecting to the network...")
            }

            override fun onUserCancelled() {
                debugLog("Cancelled")
            }
        }
        if (saved == null) {
            val primaryAdapter = WiFi.getPrimaryAdapter()
            // Try connection over primary adapter in case user connected to access point manually
            if (primaryAdapter != null) {
                debugLog("First trying connection over primary adapter...")
                val rc = tryConnectWiFi(primaryAdapter, { job -> connectCallback(job) })
                if (rc == Pak.Error.CANCELLED) return
                if (rc == 0) {
                    setIsConnected()
                    return
                }
            }

            val filter = WiFi.ApFilter()
            filter.ssidPattern = wifi.ssidPattern
            filter.password = wifi.defaultPassword
            WiFi.connectToAccessPointCompanion(filter, companionName, callback)
        } else {
            debugLog("Connecting to ${saved.bluetoothMacAddress}")
            WiFi.connectFromBSSID(saved.bluetoothMacAddress, callback)
        }
    }

    private fun connectCallback(job: ModuleJob) {
        homeModelView.connectProgress.update {
            job.progressBarValue
        }
    }

    private fun bluetoothConnectRoutine(info: List<ModuleManifest.BluetoothDiscovery>, saved: SavedDeviceEntity?) {
        if (!Bluetooth.checkPermission()) {
            homeModelView.connectRequiredAction.value = ConnectingRequiredAction.ACCEPT_PERMISSION
            Bluetooth.requestConnectPermission()
        } else if (!Bluetooth.isBluetoothEnabled()) {
            homeModelView.connectRequiredAction.value = ConnectingRequiredAction.TURN_ON_BLUETOOTH
            Bluetooth.openEnableBluetoothDialog()
        } else {
            fun doConnect(dev: Bluetooth.Device) {
                debugLog("Connecting to '${dev.name}'...")
                setBluetoothDevice(dev)
                val rc = tryConnectBluetooth(dev, saved, {job -> connectCallback(job)})
                if (rc == 0) {
                    setIsConnected()
                } else {
                    setBluetoothDevice(null)
                    disconnect("Failed to connect", rc)
                }
            }

            if (request.deviceMacAddress != null) {
                val dev = Bluetooth.fromAddress(request.deviceMacAddress!!) // wtf
                if (dev.isBonded) debugLog("Connecting to a bonded device")
                doConnect(dev)
                return
            }

            if (saved != null && saved.bluetoothMacAddress != null) {
                doConnect(Bluetooth.fromAddress(saved.bluetoothMacAddress))
                return
            }

            val filters: List<Bluetooth.BtFilter> = info.map {
                it.toBluetoothDeviceFilter()
            }

            val callback = object : Bluetooth.ScanCallback() {
                override fun onFound(device: Bluetooth.Device) {
                    doConnect(device)
                }
                override fun onFailure(reason: String) {
                    debugLog("System error: ${reason}")
                }

                override fun onCancel() {
                    debugLog("Cancelled")
                }
            }
            val rc = Bluetooth.pairWithDeviceCompanion(filters, "FudgeDevice1", null,callback)
            if (rc != 0) {
                debugLog("Bluetooth.pairWithDeviceCompanion: ${rc}")
            }
        }
    }

    fun tryConnectAgain() {
        if (initJob?.isCompleted ?: true) {
            initThread()
        }
    }

    private fun initModule(): Boolean {
        val rc = when (manifest.moduleType) {
            ModuleManifest.ModuleType.SHARED_LIBRARY -> {
                AndroidRuntime.setupSharedLibraryModule(this, manifest.scriptPath!!)
            }
            ModuleManifest.ModuleType.QUICKJS -> {
                val path = manifest.scriptPath
                if (path == null) {
                    debugLog("<error>script path not included")
                    -1
                } else {
                    var fileContents = FileLayer.readFile(path)
                    if (fileContents == null) {
                        debugLog("Failed to read ${path}")
                        -1
                    } else {
                        fileContents += 0.toByte()
                        AndroidRuntime.setupJavascriptModule(this, fileContents)
                    }
                }
            }
            ModuleManifest.ModuleType.WEBASSEMBLY -> {
                debugLog("<error>Wasm not implemented yet")
                -1
            }
        }
        if (rc != 0) {
            homeModelView.initializationError.value = true
            return true
        }
        request.chosenSetupOption?.let { setSetupOptionName(it) }
        return false
    }

    private fun initConnection() {
        homeModelView.connectRequiredAction.value = ConnectingRequiredAction.NONE
        val savedDeviceInfo = request.getSavedDeviceEntity()
        val option = request.getSetupOption()
        val transport = when {
            option != null -> option.transport
            target.wifiDiscovery != null -> ModuleManifest.Transport.WIFI_AP
            target.bluetoothDiscovery.isNotEmpty() -> ModuleManifest.Transport.BLUETOOTH
            else -> null
        }
        if (target.wifiDiscovery != null && transport == ModuleManifest.Transport.WIFI_AP) {
            wifiConnectRoutine(target.wifiDiscovery, savedDeviceInfo)
        } else if (target.bluetoothDiscovery.isNotEmpty() && transport == ModuleManifest.Transport.BLUETOOTH) {
            bluetoothConnectRoutine(target.bluetoothDiscovery, savedDeviceInfo)
        } else if (transport == ModuleManifest.Transport.LOCAL_NETWORK_UDP) {
            if (!WiFi.checkPermission()) {
                homeModelView.connectRequiredAction.value = ConnectingRequiredAction.ACCEPT_PERMISSION
                WiFi.requestConnectPermission()
                return
            }

            val primaryAdapter = WiFi.getPrimaryAdapter()
            val rc = tryConnectWiFi(primaryAdapter, {job -> connectCallback(job)})
            if (rc == 0) {
                setIsConnected()
                return
            } else {
                disconnect("Failed to connect", rc)
            }
        } else {
            val rc = findConnection({ job -> connectCallback(job) })
            if (rc == 0) {
                setIsConnected()
            } else {
                disconnect("Failed to connect", rc)
            }
        }
    }

    fun initThread() {
        initJob = CoroutineScope(Dispatchers.IO).launch {
            if (!initModule()) {
                initConnection()
            }
            initJob = null
        }
    }

    fun startMainLoop() {
        val module = this
        mainLoopJob = CoroutineScope(Dispatchers.IO).launch {
            val job = mainLoopJob
            var curr = TimeSource.Monotonic.markNow()
            while (job != null && !job.isCancelled) {
                module.onIdleTick(curr.elapsedNow().toInt(DurationUnit.MICROSECONDS))
                curr = TimeSource.Monotonic.markNow()
                delay(currentTickIntervalUs.toLong() / 1000)
            }
        }
    }

    private fun disconnect(reason: String, code: Int) {
        disconnectReason = reason
        debugLog("<error>Disconnected: ${reason}")
        disconnectedErrorCode = code
        homeModelView.goToScreen(Screen.DISCONNECTED)
    }

    @CalledFromNative
    fun forceDisconnect(reason: String, code: Int = 0) {;
        if (isConnected) {
            isConnected = false
            CoroutineScope(Dispatchers.IO).launch {
                stopAllThreads()
                withJob({}) { job ->
                    onDisconnect()
                }
                disconnect(reason, code)
            }
        }
    }

    private fun switchScreen(prev: Screen, new: Screen, callback: JobUpdateCallback): Int {
        val rc = withJob(callback) { job ->
            onSwitchScreen(prev.id, new.id, job.id)
        }
        if (new != prev) {
            when (new) {
                Screen.FILE_GALLERY -> galleryViewModel.start()
                else -> {}
            }
            when (prev) {
                Screen.FILE_GALLERY -> galleryViewModel.setPaused(true)
                Screen.INTERVALOMETER -> intervalometerModel.onShutdown()
                else -> {}
            }
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
}