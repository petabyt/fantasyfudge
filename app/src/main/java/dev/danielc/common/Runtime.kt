package dev.danielc.common

import dev.danielc.R
import dev.danielc.common.screens.ConsoleLine
import dev.danielc.common.screens.ConsoleViewModel
import dev.danielc.common.screens.MimeType
import dev.danielc.common.ui.dummyManifestList
import dev.danielc.libpak.Bluetooth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import dev.danielc.fudge.AndroidRuntime

/**
 * Module defined setting that can be updated by the user or the module
  */
data class UserSetting(
    val name: String,
    val title: String,
    var currentBooleanValue: Boolean? = null,
    val currentIntValue: Int? = null,
    val currentStringValue: String? = null,
    val intMin: Int? = null,
    val intMax: Int? = null,
    val dropDownOptions: List<String>? = null,
)

@Serializable
enum class Screen(val strId: String, val id: Int) {
    NONE("none", 0),
    CONNECT("connect", 1),
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
 * struct FileHandle
 */
data class FileHandle(
    var index: Int = 0,
    var storageName: String? = null,
)

fun getMimeType(str: String?): MimeType {
    return when (str) {
        "image/jpeg" -> MimeType.JPEG
        "image/png" -> MimeType.PNG
        "image/quicktime" -> MimeType.MOV
        else -> MimeType.FILE
    }
}

/**
 * struct FileMetadata
 */
data class FileMetadata(
    var filename: String? = null,
    val mimeType: MimeType = MimeType.FILE,
    val width: Int = 0,
    val height: Int = 0,
    val filesize: Int = 0,
    val createdDate: String? = null,
    val updatedDate: String? = null,
    val orientation: Int = 0,
) {
    constructor(filename: String?, mimeTypeString: String?, width: Int, height: Int, size: Int) : this(filename, mimeType = getMimeType(mimeTypeString), width = width, height = height, filesize = size)
}

/**
 * Property IDs for the module instance
 */
enum class ModuleProperty(val id: String) {
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
    // null if not supported or no target found
    val target: ModuleManifest.Target? = null,
    val manifest: ModuleManifest? = null,
    val isConnected: Boolean,
)

object Runtime {
    var earlyConsoleLogs: MutableList<ConsoleLine> = mutableListOf()
    var mainLog: ConsoleViewModel? = null
    var connectableDevices = listOf<ConnectableDevice>()
    var moduleManifests = listOf<ModuleManifest>()
    var moduleInstances = mutableMapOf<Int, ModuleInstance>()
    private var moduleCounter = 0

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
        val devices = Bluetooth.getBondedDevices(Bluetooth.getDefaultAdapter())
        val list = mutableListOf<ConnectableDevice>()
        for (d in devices) {
            // TODO filter devices through modules
            list += ConnectableDevice(d.name, dummyManifestList[0].targets[0], dummyManifestList[0], d.isConnected)
        }
        //connectableDevices = list
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
        if (mainLog == null) {
            earlyConsoleLogs.add(ConsoleLine(s))
        }
        mainLog?.addLine(s)
    }

    fun loadModulesFromManifests(pathList: List<String>) {
        val list = mutableListOf<ModuleManifest>()
        list += ModuleManifest(
            name = "dummymod",
            description = "Test module that calls some internal C code",
            moduleType = ModuleManifest.ModuleType.DUMMY_MODULE,
            targets = listOf(
                ModuleManifest.Target(
                    company = "Dummy Company",
                    deviceId = Device.GAME_CONTROLLER
                )
            ),
        )

        list += ModuleManifest(
            name = "libfuji",
            description = "All Fujifilm cameras",
            moduleType = ModuleManifest.ModuleType.LIBFUJI,
            targets = listOf(
                ModuleManifest.Target(
                    company = "Fujifilm",
                    deviceId = Device.PROFESSIONAL_CAMERA,
                    products = listOf("X-T1", "X-T2", "X-T3", "X-T4", "X-T5"),
                    wifiDiscovery = ModuleManifest.WiFiDiscovery("FUJIFILM-.*"),
                    bleDiscovery = ModuleManifest.BleDiscovery(namePattern = "FUJIFILM-.*",
                        serviceUuids = listOf("af854c2e-b214-458e-97e2-912c4ecf2cb8"),
//                        mfgData = byteArrayOf(0xD8.toByte(), 0x04, 0x02, 0xA0.toByte(), 0x48, 0x21,
//                            0x80.toByte()
//                        ),
//                        mfgDataMask = byteArrayOf(0xff.toByte())
                    ),
                    setupOptions = listOf(
                        ModuleManifest.SetupOption("wifi", "WiFi (Legacy)", ModuleManifest.Transport.WIFI_AP),
                        ModuleManifest.SetupOption("local-network", "PC AutoSave & Wireless Tether Shoot", ModuleManifest.Transport.LOCAL_NETWORK_UPNP_LISTEN),
                        ModuleManifest.SetupOption("bluetooth", "Bluetooth", ModuleManifest.Transport.BLE),
                        ModuleManifest.SetupOption("usb", "USB", ModuleManifest.Transport.USB),
                    ),
                )
            ),
        )

        list += ModuleManifest(
            name = "cmf-nothing-audio",
            description = "CMF Nothing Audio devices",
            moduleType = ModuleManifest.ModuleType.CMF_NOTHING,
            targets = listOf(
                ModuleManifest.Target(
                    company = "Nothing",
                    deviceId = Device.EARBUDS,
                    products = listOf("Buds Pro 2", "Buds 2")
                )
            ),
        )

        list += ModuleManifest(
            name = "goveelife",
            description = "GoveeLife smart home devices",
            moduleType = ModuleManifest.ModuleType.GOVEELIFE,
            targets = listOf(
                ModuleManifest.Target(
                    company = "GoveeLife",
                    deviceId = Device.GENERIC_HOME_DEVICE,
                    products = listOf("thermometer"),
                    bleDiscovery = ModuleManifest.BleDiscovery(
                        namePattern = "GVH...._....",
                        serviceUuids = listOf("0000ec88-0000-1000-8000-00805f9b34fb"),
                        mfgData = byteArrayOf(0x4c, 0x0, 0x02, 0x15, 0x49, 0x4E, 0x54, 0x45, 0x4C, 0x4C, 0x49, 0x5F, 0x52, 0x4F, 0x43, 0x4B, 0x53, 0x5F, 0x48, 0x57, 0x50, 0x75,
                            0xF2.toByte(),
                            0xFF.toByte(), 0x0C),
                        // byteArrayOf(0x01, 0x00, 0x01, 0x01, 0x14, 0x48, 0x46, 0xA8.toByte())
                    )
                )
            ),
        )

        for (filename in pathList) {
            try {
                val text = AndroidRuntime.readAssetsFile(filename)
                val obj: JsonElement = Json.parseToJsonElement(String(text))
                val root = obj.jsonObject

                val jsonTarget = root["targets"]?.jsonArray

                val targets = mutableListOf<ModuleManifest.Target>()
                if (jsonTarget != null) {
                    for (target in jsonTarget) {
                        targets += ModuleManifest.Target(
                            products = Json.decodeFromJsonElement<List<String>>(target.jsonObject["products"]!!),
                            deviceId = Device.fromId(target.jsonObject["deviceType"]?.jsonPrimitive?.content)!!
                        )
                    }
                }
                val manifest = ModuleManifest(
                    name = root["name"]?.jsonPrimitive?.content!!,
                    description = root["description"]?.jsonPrimitive?.content,
                    author = root["author"]?.jsonPrimitive?.content!!,
                    authorUrl = root["authorUrl"]?.jsonPrimitive?.content,
                    version = root["version"]?.jsonPrimitive?.int!!,
                    isDraft = root["isDraft"]?.jsonPrimitive?.booleanOrNull == true,
                    targets = targets,
                )

                list += manifest
            } catch (e: Exception) {
                logGlobalLine("Error parsing manifest: $filename")
                logGlobalLine(e.toString())
            }
        }
        moduleManifests = list
    }
}