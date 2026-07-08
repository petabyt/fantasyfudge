package dev.danielc.common

import dev.danielc.R

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
 * Constructed manually or loaded from JSON in modules folder.
 *
 * This may have 'discovery info', which can be used to dynamically match
 * available devices (advertisements, paired devices) to modules without having
 * to initialize a module instance and use a lot of ram
 */
data class ModuleManifest(
    val name: String,
    val description: String? = null,
    val website: String? = null,
    val author: String? = null,
    val version: Int = 0,
    val requiredRuntimeVersion: Int? = null,
    val runtimeVersion: Int = 0,
    val publicKey: String? = null,
    val isDraft: Boolean = false,
    val moduleType: ModuleType = ModuleType.WEBASSEMBLY,
    val scriptPath: String? = null,
    val targets: List<Target> = emptyList()
) {
    enum class Transport(value: Int) {
        BLUETOOTH(1),
        USB(2),
        WIFI_AP(3),
        USB_DEVICE_MODE(4),
        HOST_WIFI_AP(5),
        INTERNET(6),
    }

    enum class ModuleType {
        QUICKJS,
        WEBASSEMBLY,
        SHARED_LIBRARY;
        fun getDesc(): String {
            return when (this) {
                QUICKJS -> "Javascript"
                WEBASSEMBLY -> "Webassembly"
                SHARED_LIBRARY -> "Shared library (internal)"
            }
        }
    }
    data class SetupOption(
        val name: String,
        val title: String,
        val transport: Transport? = null,
    )
    data class Target(
        val deviceId: Device = Device.PROFESSIONAL_CAMERA,
        val company: String = "",
        val summary: String? = null,
        val products: List<String> = emptyList(),
        val setupOptions: List<SetupOption> = emptyList(),
        val wifiDiscovery: WiFiDiscovery? = null,
        val bluetoothDiscovery: BluetoothDiscovery? = null,
    )
    data class WiFiDiscovery(
        val ssidPattern: String,
    )
    @Suppress("ArrayInDataClass")
    data class BluetoothDiscovery(
        val namePattern: String? = null,
        val isClassic: Boolean = false,
        val mfgData: ByteArray? = null,
        val mfgDataMask: ByteArray? = null,
        val serviceUuids: List<String> = emptyList(),
    )
    data class UsbDiscovery(
        val pid: Int? = null,
        val vid: Int? = null,
        val usbClass: Int? = null,
    )
}