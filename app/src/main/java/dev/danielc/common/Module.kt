package dev.danielc.common
import dev.danielc.R

typealias Job = Int

class Module {
    enum class PakDevice(val id: String) {
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
            fun fromId(id: String?): PakDevice? {
                return entries.find { it.id == id }
            }
        }

        fun getIcon(): Int {
            return when (this) {
                PakDevice.PROFESSIONAL_CAMERA -> R.drawable.baseline_photo_camera_24
                PakDevice.ACTION_CAMERA -> R.drawable.baseline_photo_camera_24
                PakDevice.DASHCAM -> R.drawable.outline_camera_video_24
                PakDevice.GENERIC_CAMERA -> R.drawable.baseline_photo_camera_24
                PakDevice.WIFI_SD_CARD -> R.drawable.outline_sd_card_24
                PakDevice.DOORBELL -> R.drawable.outline_general_device_24
                PakDevice.GENERIC_HOME_DEVICE -> R.drawable.outline_general_device_24
                PakDevice.DESK -> R.drawable.outline_general_device_24
                PakDevice.GENERIC_FURNITURE -> R.drawable.outline_general_device_24
                PakDevice.PRINTER_3D -> R.drawable.outline_general_device_24
                PakDevice.HEADPHONES -> R.drawable.outline_general_device_24
                PakDevice.EARBUDS -> R.drawable.outline_general_device_24
                PakDevice.SPEAKERS -> R.drawable.outline_general_device_24
                PakDevice.GENERIC_AUDIO -> R.drawable.outline_general_device_24
                PakDevice.SMART_GLASSES -> R.drawable.outline_general_device_24
                PakDevice.SMART_TV -> R.drawable.outline_connected_tv_24
                PakDevice.SMARTWATCH -> R.drawable.outline_general_device_24
                PakDevice.GENERIC_MEDICAL_WEARABLE -> R.drawable.outline_general_device_24
                PakDevice.GENERIC_EXERCISE_MACHINE -> R.drawable.outline_general_device_24
                PakDevice.POWER_TOOL -> R.drawable.outline_general_device_24
                PakDevice.GAME_CONTROLLER -> R.drawable.outline_general_device_24
                PakDevice.DRONE -> R.drawable.outline_general_device_24
                PakDevice.GENERIC_REMOTE_CONTROL -> R.drawable.outline_general_device_24
                PakDevice.SCOOTER -> R.drawable.outline_general_device_24
                PakDevice.BICYCLE -> R.drawable.outline_general_device_24
                PakDevice.GENERIC_RIDEABLE -> R.drawable.outline_general_device_24
                PakDevice.AUTOMOTIVE_INFOTAINMENT -> R.drawable.outline_directions_car_24
                PakDevice.AUTOMOTIVE_DIAGNOSTIC -> R.drawable.outline_car_repair_24
                PakDevice.GENERIC_AUTOMOTIVE -> R.drawable.outline_directions_car_24
            }
        }
    }

    data class RememberedDevice(
        val uniqueId: String,
        val name: String,
    ) {

    }

    data class Target(
        val deviceId: PakDevice = PakDevice.PROFESSIONAL_CAMERA,
        val companies: List<String> = emptyList(),
        val products: List<String> = emptyList(),
    )

    data class Manifest(
        val name: String,
        val description: String,
        val author: String = "Daniel Cook",
        val target: Target = Target()
    )

    val currentScreen: String = "connect-wifi"
    fun cancelJob(job: Job) {

    }
    constructor(manifest: Manifest) {

    }
}