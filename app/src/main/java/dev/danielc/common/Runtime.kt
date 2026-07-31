package dev.danielc.common

import dev.danielc.common.screens.ConsoleModel
import dev.danielc.common.screens.LocalGalleryViewModel
import dev.danielc.fudge.AndroidRuntime
import dev.danielc.fudge.AndroidRuntime.getDatabase
import dev.danielc.fudge.FileLayer
import dev.danielc.libpak.Bluetooth
import dev.danielc.libpak.Pak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * This is used over ViewModel so that it can be easily stowed away and kept running in the background
 * without having to keep it bound to a composable
 */
open class BackgroundViewModel {
    val scope = CoroutineScope(Dispatchers.IO)
    open fun onTrimMemory() {
        println("TODO: Trim memory")
    }
    /** Similar to onCleared */
    open fun onShutdown() {
        println("TODO: Shut down")
    }
    open fun onResume() {
        println("TODO: Unpaused")
    }
}

/**
 * A connectable device found or exposed
 */
data class ConnectableDevice(
    val name: String,
    val target: ModuleManifest.Target,
    val manifest: ModuleManifest,
    val macAddress: String? = null,
)

object Runtime {
    private val _trimMemorySignal = MutableSharedFlow<Unit>()
    val trimMemorySignal = _trimMemorySignal.asSharedFlow()
    suspend fun emitMemoryTrimSignal() { _trimMemorySignal.emit(Unit) }
    val mainLog = ConsoleModel()
    val localGalleryViewModel = LocalGalleryViewModel()
    var connectableDevices = listOf<ConnectableDevice>()
    var moduleManifests = listOf<ModuleManifest>()
    var savedDevices: Flow<List<SavedDeviceEntity>> = MutableStateFlow(emptyList())
    var moduleInstances = mutableMapOf<Int, ModuleInstance>()
    private var moduleCounter = 0

    fun findSavedDeviceEntity(id: String): SavedDeviceEntity? {
        val list = getDatabase().deviceDao().getAllDevicesList()
        return list.find { it.uniqueIdentifier == id }
    }

    fun errorCodeToString(rc: Int): String {
        return when (rc) {
            Pak.Error.IO -> "IO Error"
            Pak.Error.CANCELLED -> "Cancelled"
            Pak.Error.DISCONNECTED -> "Disconnected"
            Pak.Error.NO_CONNECTION -> "Can't connect"
            Pak.Error.PERMISSION -> "Permission error"
            Pak.Error.UNSUPPORTED -> "Unsupported"
            Pak.Error.UNIMPLEMENTED -> "Unimplemented method"
            Pak.Error.NON_FATAL -> "Non fatal error"
            else -> ""
        }
    }

    fun addModuleInstance(mod: ModuleInstance): Int {
        moduleInstances.put(++moduleCounter, mod)
        return moduleCounter
    }

    fun removeModuleInstance(mod: ModuleInstance) {
        val key = moduleInstances.entries.find { it.value == mod }?.key
        if (key != null) {
            moduleInstances.remove(key)
        }
    }

    // This doesn't work well in practice since per-app bonds are not available system wide
    fun refreshAgainstBondedDevices() {
        // Match installed manifests with bonded bluetooth devices
        val devices = Bluetooth.getBondedDevices(Bluetooth.getDefaultAdapter())
        val list = mutableListOf<ConnectableDevice>()
        fun checkServiceUuids(b: ModuleManifest.BluetoothDiscovery, d: Bluetooth.Device): Boolean {
            // Will return true even if both sets are empty
            val serviceUuids = d.serviceUuids
            //if (serviceUuids.isEmpty() || b.serviceUuids.isEmpty()) return false
            for (f in b.serviceUuids) {
                if (!serviceUuids.contains(f)) return false
            }
            return true
        }
        fun matchAgainstManifest(m: ModuleManifest, d: Bluetooth.Device): Boolean {
            for (t in m.targets) {
                for (b in t.bluetoothDiscovery) {
                    // TODO: async try to fetch UUIDs over sdp
                    if (!checkServiceUuids(b, d)) continue
                    if (b.namePattern != null) {
                        if (!Regex(b.namePattern).matches(d.name)) continue
                    } else {
                        continue
                    }

                    list += ConnectableDevice(d.name, t, m, d.address)
                    return true
                }
            }
            return false
        }
        for (d in devices) {
            //d.refreshSdpUuids()
            for (m in moduleManifests) {
                if (matchAgainstManifest(m, d)) break
            }
            //d.closeAll()
        }
        connectableDevices = list
    }

    fun refreshConnectableDevices() {
        // Sense nearby devices in database
        savedDevices = getDatabase().deviceDao().getAllDevices()
        val savedDevices = getDatabase().deviceDao().getAllDevicesList()
        for (d in savedDevices) {
//            if (d.associationId != null)
//                Bluetooth.senseNearbyDevice(d.associationId)
            if (d.bluetoothMacAddress != null)
                Bluetooth.startListeningToDevicePresence(d.bluetoothMacAddress)
        }
    }

    fun getManifestFromName(name: String): ModuleManifest? {
        for (m in moduleManifests) {
            if (m.name == name) return m
        }
        return null
    }

    fun refreshManifests() {
        val manifests = AndroidRuntime.getJsonManifestList()
        loadModulesFromManifests(manifests)
    }

    fun logGlobalLine(s: String) {
        mainLog.addLine(s)
    }

    fun loadModulesFromManifests(pathList: List<String>) {
        val list = mutableListOf<ModuleManifest>()
        list += ModuleManifest(
            name = "dummymod",
            moduleType = ModuleManifest.ModuleType.SHARED_LIBRARY,
            scriptPath = "libdummy.so",
            targets = listOf(
                ModuleManifest.Target(
                    company = "Dummy",
                    summary = "Fake session that can be used to test this app",
                    deviceId = Device.GAME_CONTROLLER
                )
            ),
        )

        list += ModuleManifest(
            name = "libfuji",
            description = "libfuji port",
            website = "https://github.com/petabyt/libfuji",
            author = "Daniel Cook",
            moduleType = ModuleManifest.ModuleType.SHARED_LIBRARY,
            scriptPath = "libfuji.so",
            targets = listOf(
                ModuleManifest.Target(
                    company = "Fujifilm",
                    summary = "All Fujifilm digital cameras",
                    deviceId = Device.PROFESSIONAL_CAMERA,
                    products = listOf("X-T1", "X-T2", "X-T3", "X-T4", "X-T5"),
                    wifiDiscovery = ModuleManifest.WiFiDiscovery("FUJIFILM-.*"),
                    bluetoothDiscovery = listOf(ModuleManifest.BluetoothDiscovery(
                        serviceUuids = listOf("117c4142-edd4-4c77-8696-dd18eebb770a"),
//                        mfgData = byteArrayOf(0xD8.toByte(), 0x04, 0x02, 0xA0.toByte(), 0x48, 0x21,
//                            0x80.toByte()
//                        ),
//                        mfgDataMask = byteArrayOf(0xff.toByte())
                    ), ModuleManifest.BluetoothDiscovery(
                        serviceUuids = listOf("a9d2b304-e8d6-4902-8336-352b772d7597")
                    )),
                    setupOptions = listOf(
                        ModuleManifest.SetupOption("wifi", "WiFi (Legacy)", ModuleManifest.Transport.WIFI_AP),
                        ModuleManifest.SetupOption("local-network", "PC AutoSave & Wireless Tether Shoot", ModuleManifest.Transport.LOCAL_NETWORK_UDP),
                        ModuleManifest.SetupOption("bluetooth", "Bluetooth", ModuleManifest.Transport.BLUETOOTH),
                        ModuleManifest.SetupOption("usb", "USB", ModuleManifest.Transport.USB),
                    ),
                )
            ),
        )

        list += ModuleManifest(
            name = "cmf-nothing-audio",
            moduleType = ModuleManifest.ModuleType.SHARED_LIBRARY,
            scriptPath = "libcmfnothingaudio.so",
            targets = listOf(
                ModuleManifest.Target(
                    company = "Nothing",
                    summary = "CMF Nothing Audio devices",
                    deviceId = Device.EARBUDS,
                    products = listOf("Buds Pro 2", "Buds 2"),
                    bluetoothDiscovery = listOf(ModuleManifest.BluetoothDiscovery(
                        isClassic = false,
                        namePattern = "CMF.*",
                        mfgData = byteArrayOf(0x31, 0x44, 0x42, 0xee.toByte(), 0xbe.toByte(), 0x2c),
                        mfgDataMask = byteArrayOf(0xff.toByte(), 0xff.toByte()),
                    ))
                )
            ),
        )

        list += ModuleManifest(
            name = "goveelife",
            moduleType = ModuleManifest.ModuleType.SHARED_LIBRARY,
            scriptPath = "libgoveelife.so",
            targets = listOf(
                ModuleManifest.Target(
                    company = "GoveeLife",
                    summary = "GoveeLife smart home devices",
                    deviceId = Device.GENERIC_HOME_DEVICE,
                    products = listOf("thermometer"),
                    bluetoothDiscovery = listOf(ModuleManifest.BluetoothDiscovery(
                        namePattern = "GVH...._....",
                        serviceUuids = listOf("0000ec88-0000-1000-8000-00805f9b34fb"),
                        mfgData = byteArrayOf(0x4c, 0x0, 0x02, 0x15, 0x49, 0x4E, 0x54, 0x45, 0x4C, 0x4C, 0x49, 0x5F, 0x52, 0x4F, 0x43, 0x4B, 0x53, 0x5F, 0x48, 0x57, 0x50, 0x75,
                            0xF2.toByte(),
                            0xFF.toByte(), 0x0C),
                        // byteArrayOf(0x01, 0x00, 0x01, 0x01, 0x14, 0x48, 0x46, 0xA8.toByte())
                    ))
                )
            ),
        )

        for (filename in pathList) {
            try {
                val text = FileLayer.readFile(filename)
                if (text == null) {
                    logGlobalLine("Failed to read ${filename}")
                    continue
                }
                val obj: JsonElement = Json.parseToJsonElement(String(text))
                val root = obj.jsonObject

                val jsonTarget = root["targets"]?.jsonArray

                val targets = mutableListOf<ModuleManifest.Target>()
                if (jsonTarget != null) {
                    for (target in jsonTarget) {
                        var t = ModuleManifest.Target(
                            company = target.jsonObject["company"]?.jsonPrimitive?.content ?: throw Error("Missing company field"),
                            summary = target.jsonObject["summary"]?.jsonPrimitive?.content,
                            products = Json.decodeFromJsonElement<List<String>>(target.jsonObject["products"] ?: throw Error("Missing products field")),
                            deviceId = Device.fromId(target.jsonObject["deviceType"]?.jsonPrimitive?.content) ?: throw Error("Missing deviceType field")
                        )

                        val jsonWiFiFilter = target.jsonObject["wifiFilter"]?.jsonObject
                        if (jsonWiFiFilter != null) {
                            t = t.copy(
                                wifiDiscovery = ModuleManifest.WiFiDiscovery(
                                    ssidPattern = jsonWiFiFilter.jsonObject["ssidPattern"]?.jsonPrimitive?.content ?: throw Error("Missing ssidPattern field"),
                                    defaultPassword = jsonWiFiFilter.jsonObject["defaultPassword"]?.jsonPrimitive?.content
                                )
                            )
                        }

                        targets += t
                    }
                }
                val manifest = ModuleManifest(
                    name = root["name"]?.jsonPrimitive?.content ?: throw Error("missing name field"),
                    description = root["description"]?.jsonPrimitive?.content,
                    author = root["author"]?.jsonPrimitive?.content,
                    website = root["website"]?.jsonPrimitive?.content,
                    scriptPath = filename.replaceAfterLast("/", root["modulePath"]?.jsonPrimitive?.content ?: throw Error("missing modulePath field")),
                    moduleType = when (root["moduleType"]?.jsonPrimitive?.content ?: throw Error("missing moduleType field")) {
                        "js" -> ModuleManifest.ModuleType.QUICKJS
                        "wasm" -> ModuleManifest.ModuleType.WEBASSEMBLY
                        else -> ModuleManifest.ModuleType.SHARED_LIBRARY
                    },
                    version = root["version"]?.jsonPrimitive?.int ?: throw Error("Missing version field"),
                    isDraft = root["isDraft"]?.jsonPrimitive?.booleanOrNull == true,
                    targets = targets,
                )

                list += manifest
            } catch (e: Exception) {
                logGlobalLine("Error parsing manifest $filename")
                logGlobalLine(e.message ?: "")
            }
        }
        moduleManifests = list
    }
}