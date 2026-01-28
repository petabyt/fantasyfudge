package dev.danielc.common
import dev.danielc.R
import kotlinx.serialization.Serializable

typealias Job = Int

class Module(val nativeModule: NativeModule) {
    enum class ModuleType {
        QUICKJS,
        WEBASSEMBLY,
        NATIVE,
    }

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
                Device.ACTION_CAMERA -> R.drawable.baseline_photo_camera_24
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
                Device.SMART_GLASSES -> R.drawable.outline_general_device_24
                Device.SMART_TV -> R.drawable.outline_connected_tv_24
                Device.SMARTWATCH -> R.drawable.outline_general_device_24
                Device.GENERIC_MEDICAL_WEARABLE -> R.drawable.outline_general_device_24
                Device.GENERIC_EXERCISE_MACHINE -> R.drawable.outline_general_device_24
                Device.POWER_TOOL -> R.drawable.outline_tools_power_drill_24
                Device.GAME_CONTROLLER -> R.drawable.outline_general_device_24
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

    data class RememberedDevice(
        val uniqueId: String,
        val name: String,
    )

    data class Target(
        val deviceId: Device = Device.PROFESSIONAL_CAMERA,
        val companies: List<String> = emptyList(),
        val products: List<String> = emptyList(),
    )

    data class Manifest(
        val name: String,
        val description: String,
        val author: String = "Daniel Cook",
        val target: Target = Target()
    )
}

class ModuleInstance(mod: Module) {
    val module: Module = mod
    val currentScreen: String = ""
    fun cancelJob(job: Job) {

    }
}