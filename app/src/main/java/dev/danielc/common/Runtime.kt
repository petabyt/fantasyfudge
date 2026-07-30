package dev.danielc.common

import dev.danielc.R
import dev.danielc.common.screens.ConsoleModel
import dev.danielc.common.screens.LocalGalleryViewModel
import dev.danielc.common.screens.MimeType
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
import kotlinx.serialization.Serializable
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
}

@Suppress("ArrayInDataClass")
data class SavedDeviceInfo(
    val uniqueIdentifier: String,
    val name: String,
    val privateData: ByteArray?,
)

/**
 * PakModule defined setting or widget that can be updated by the user or the module
  */
sealed interface DashboardPane {
    val args: Properties
    data class Properties(
        val name: String,
        val title: String,
    )
    data class Button(override val args: Properties): DashboardPane
    data class BooleanSetting(override val args: Properties, val value: Boolean): DashboardPane
    data class IntSetting(override val args: Properties, val value: Int): DashboardPane
    data class SliderSetting(override val args: Properties, val value: Int, val min: Int, val max: Int): DashboardPane
    data class DropdownSetting(override val args: Properties, val index: Int, val options: List<String>): DashboardPane {
        constructor(args: Properties, index: Int, options: Array<String>) : this(args, index, options.toList())
    }
    @Suppress("ArrayInDataClass")
    data class Graph(
        override val args: Properties,
        val points: IntArray,
    ): DashboardPane
}

enum class Command(val cmd: String) {
    PAK_CMD_SHUTTER_DOWN("shutter-down"),
    PAK_CMD_SHUTTER_UP("shutter-up"),
    PAK_CMD_FOCUS_DOWN("focus-down"),
    PAK_CMD_FOCUS_UP("focus-up"),
}

data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val satellites: Int,
)

@Serializable
enum class Screen(val strId: String, val id: Int) {
    NONE("none", 0),
    CONNECT("connect", 1),
    CONNECT_SECONDARY("connecting-secondary", 1),
    SETUP("setup", 10),

    CONSOLE("console", 100),
    DASHBOARD("dashboard", 101),
    FILE_GALLERY("filegallery", 102),
    FILE_VIEWER("fileviewer", 103),
    GEOTAGGING("geotagging", 104),
    LIVEVIEW("liveview", 105),
    LIVE_FEED("livefeed", 106),
    INTERVALOMETER("intervalometer", 107),
    DISCONNECTED("disconnected", 200);

    companion object {
        fun fromId(id: Int?): Screen? {
            return Screen.entries.find { it.id == id }
        }
        fun fromStrId(id: String): Screen? {
            return Screen.entries.find { it.strId == id }
        }
    }

    fun getIcon(): Int {
        return when (this) {
            CONNECT -> R.drawable.baseline_wifi_tethering_24
            CONSOLE -> R.drawable.baseline_terminal_24
            DASHBOARD -> R.drawable.outline_home_24
            FILE_GALLERY -> R.drawable.outline_photo_library_24
            FILE_VIEWER -> R.drawable.outline_photo_library_24
            GEOTAGGING -> R.drawable.outline_globe_location_pin_24
            LIVEVIEW -> R.drawable.outline_smart_display_24
            LIVE_FEED -> R.drawable.outline_dynamic_feed_24
            INTERVALOMETER -> R.drawable.outline_timelapse_24
            NONE -> R.drawable.baseline_question_mark_24
            else -> R.drawable.baseline_question_mark_24
        }
    }

    fun getName(): String {
        return when (this) {
            CONNECT -> "Connect"
            CONSOLE -> "Console"
            DASHBOARD -> "Dashboard"
            FILE_GALLERY -> "Gallery"
            FILE_VIEWER -> "Viewer"
            GEOTAGGING -> "Geotagging"
            LIVEVIEW -> "Liveview"
            LIVE_FEED -> "Live feed"
            INTERVALOMETER -> "Intervalometer"
            NONE -> "None"
            else -> "?"
        }
    }
}

/**
 * struct PakFileHandle
 */
data class FileHandle(
    var index: Int = 0,
    var storageName: String? = null,
)

/**
 * struct PakFileMetadata
 */
data class FileMetadata(
    var filename: String? = null,
    val mimeType: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val filesize: Int = 0,
    val createdDate: String? = null,
    val updatedDate: String? = null,
    val orientation: Int = 0,
) {
    fun getMimeType(): MimeType {
        return MimeType.fromString(this.mimeType.orEmpty())
    }
}

/**
 * Property IDs for the module instance
 */
enum class ModuleProperty(val id: String) {
    TEMPERATURE("temperature"),
    HUMIDITY("humidity"),
    NAME_OF_DEVICE("name"),
    FIRMWARE_VERSION("firmware-version"),
    BATTERY_MAIN("battery-main"),
    BATTERY_LEFT("battery-left"),
    BATTERY_RIGHT("battery-right");
    companion object {
        fun fromId(id: String?): ModuleProperty? {
            return ModuleProperty.entries.find { it.id == id }
        }
    }
}

/**
 * A connectable device found or exposed by the device
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

    fun refreshConnectableDevices() {
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

        // Sense nearby devices in database
        savedDevices = getDatabase().deviceDao().getAllDevices()
        val savedDevices = getDatabase().deviceDao().getAllDevicesList()
        for (d in savedDevices) {
//            if (d.associationId != null)
//                Bluetooth.senseNearbyDevice(d.associationId)
            if (d.bluetoothMacAddress != null)
                Bluetooth.senseNearbyDevice(d.bluetoothMacAddress)
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