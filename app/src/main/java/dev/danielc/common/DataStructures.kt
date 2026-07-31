package dev.danielc.common
import dev.danielc.R
import dev.danielc.common.screens.MimeType
import kotlinx.serialization.Serializable

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
            return entries.find { it.id == id }
        }
        fun fromStrId(id: String): Screen? {
            return entries.find { it.strId == id }
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
            return entries.find { it.id == id }
        }
    }
}