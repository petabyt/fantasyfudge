package dev.danielc.common
import dev.danielc.R
import dev.danielc.common.screens.ConsoleStateModel
import dev.danielc.common.screens.DashboardSettingPane
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class Screen(val strId: String, val id: Int) {
    CONNECT("connect", 1),
    CONSOLE("console", 100),
    DASHBOARD("dashboard", 101),
    FILE_GALLERY("filegallery", 102),
    FILE_VIEWER("fileviewer", 103),
    GEOTAGGING("geotagging", 104),
    LIVEVIEW("liveview", 105),
    LIVE_FEED("livefeed", 106);

    companion object {
        fun fromId(id: Int?): Screen? {
            return Screen.entries.find { it.id == id }
        }
    }

    fun getIcon(): Int {
        return when (this) {
            Screen.CONNECT -> R.drawable.baseline_wifi_tethering_24
            Screen.CONSOLE -> R.drawable.baseline_terminal_24
            Screen.DASHBOARD -> R.drawable.outline_home_24
            Screen.FILE_GALLERY -> R.drawable.outline_photo_library_24
            Screen.FILE_VIEWER -> R.drawable.outline_photo_library_24
            Screen.GEOTAGGING -> R.drawable.outline_globe_location_pin_24
            Screen.LIVEVIEW -> R.drawable.outline_smart_display_24
            Screen.LIVE_FEED -> R.drawable.outline_dynamic_feed_24
        }
    }

    fun getName(): String {
        return when (this) {
            Screen.CONNECT -> "Connect"
            Screen.CONSOLE -> "Console"
            Screen.DASHBOARD -> "Dashboard"
            Screen.FILE_GALLERY -> "Gallery"
            Screen.FILE_VIEWER -> "Viewer"
            Screen.GEOTAGGING -> "Geotagging"
            Screen.LIVEVIEW -> "Liveview"
            Screen.LIVE_FEED -> "Live feed"
        }
    }
}

enum class ModuleProperty(val id: String) {
    NAME_OF_DEVICE("name"),
    FIRMWARE_VERSION("firmware-version");
}

typealias JobUpdateCallback = (Job) -> Unit

@Serializable
data class Job(
    val onUpdate: JobUpdateCallback,
    val moduleInstance: SerializableModuleInstance,
    val id: Int,
    var progressBarValue: Int = 100,
    var isCancelled: Boolean = false,
    var isFinished: Boolean = false,
)

object Runtime {
    var mainLog = ConsoleStateModel()
    var moduleManifests: List<ModuleManifest> = emptyList()
    var moduleInstances: List<ModuleInstance> = emptyList()
    var jobs: List<Job> = emptyList()
    var jobCounter = 0

    fun addModuleInstance(mod: ModuleInstance): Int {
        moduleInstances += mod
        return moduleInstances.lastIndex
    }

    var tempConnection: ModuleInstance? = null
    fun openTempConnection() {
        tempConnection = NativeRuntime.getDummyModule(moduleManifests[0])

        // TODO: Replace with C code
        tempConnection!!.homeModelView.setProperty(ModuleProperty.NAME_OF_DEVICE, "Dummy Device")
        tempConnection!!.homeModelView.setProperty(ModuleProperty.FIRMWARE_VERSION, "v5.7")
        tempConnection!!.homeModelView.addSettingPane(DashboardSettingPane(
            settingName = "A custom setting",
            currentBooleanValue = true,
        ))

    }

    fun createJob(mod: SerializableModuleInstance, onUpdate: JobUpdateCallback): Job {
        val job = Job(
            moduleInstance = mod,
            id = jobCounter++,
            onUpdate = onUpdate
        )
        jobs += job
        return job
    }

    fun loadModulesFromManifests(list: List<String>) {
        moduleManifests += ModuleManifest(
            name = "Dummy Module",
            description = "Test module that calls some internal C code",
            target = ModuleManifest.Target(
                deviceId = Device.GAME_CONTROLLER
            ),
        )

        for (filename in list) {
            val text = NativeRuntime.readAssetsFile(filename)
            val obj: JsonElement = Json.parseToJsonElement(String(text))
            val root = obj.jsonObject

            val jsonTarget = root["target"]?.jsonObject
            try {
                var target: ModuleManifest.Target? = null
                if (jsonTarget != null) {
                    target = ModuleManifest.Target(
                        companies = Json.decodeFromJsonElement<List<String>>(jsonTarget["companies"]!!),
                        deviceId = Device.fromId(jsonTarget["deviceType"]?.jsonPrimitive?.content)!!
                    )
                }
                val manifest = ModuleManifest(
                    name = root["name"]?.jsonPrimitive?.content!!,
                    description = root["description"]?.jsonPrimitive?.content,
                    author = root["author"]?.jsonPrimitive?.content!!,
                    authorUrl = root["authorUrl"]?.jsonPrimitive?.content,
                    version = root["version"]?.jsonPrimitive?.int!!,
                    isDraft = root["isDraft"]?.jsonPrimitive?.booleanOrNull == true,
                    target = target,
                )

                moduleManifests += manifest
            } catch (e: Exception) {
                mainLog.addLine("Error parsing manifest: $filename")
                mainLog.addLine(e.toString())
            }
        }
    }
}