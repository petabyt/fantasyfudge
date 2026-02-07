package dev.danielc.common
import dev.danielc.R
import dev.danielc.common.Module.Device
import dev.danielc.common.screens.ConsoleStateModel
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class Screen(val id: String) {
    CONNECT("connect"),
    CONSOLE("console"),
    DASHBOARD("dashboard"),
    FILE_GALLERY("filegallery"),
    FILE_VIEWER("fileviewer"),
    GEOTAGGING("geotagging"),
    LIVEVIEW("liveview"),
    LIVE_FEED("livefeed");

    companion object {
        fun fromId(id: String?): Device? {
            return Device.entries.find { it.id == id }
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

@Serializable
data class Job(
    val moduleInstance: SerializableModuleInstance,
    val id: Int,
    var progressBarValue: Int = 100,
    var isCancelled: Boolean = false,
    var isFinished: Boolean = false,
    val onFinished: () -> Unit,
)

object Runtime {
    var mainLog = ConsoleStateModel()
    var modules: List<Module> = emptyList()
    val moduleInstances: List<ModuleInstance> = emptyList()
    var jobs: List<Job> = emptyList()
    var jobCounter = 0

    var tempConnection: ModuleInstance? = null

    fun openTempConnection() {
        tempConnection = ModuleInstance(modules[0])
    }

    fun createJob(mod: SerializableModuleInstance): Job {
        val job = Job(
            moduleInstance = mod,
            id = jobCounter++,
            onFinished = {}
        )
        jobs += job
        return job
    }

    fun loadModulesFromManifests(list: List<String>) {
        modules += Module(Module.Manifest(
            name = "Dummy Module",
            description = "Test module that calls some internal C code",
            target = Module.Target(
                deviceId = Module.Device.GAME_CONTROLLER
            )),
            createNativeModuleInstance = {
                NativeRuntime.getDummyModule()
            }
        )

        for (text in list) {
            val obj: JsonElement = Json.parseToJsonElement(text)
            val root = obj.jsonObject

            val jsonTarget = root["target"]?.jsonObject
            try {
                var target: Module.Target? = null
                if (jsonTarget != null) {
                    target = Module.Target(
                        companies = Json.decodeFromJsonElement<List<String>>(jsonTarget["companies"]!!),
                        deviceId = Module.Device.fromId(jsonTarget["deviceType"]?.jsonPrimitive?.content)!!
                    )
                }
                val manifest = Module.Manifest(
                    name = root["name"]?.jsonPrimitive?.content!!,
                    description = root["description"]?.jsonPrimitive?.content,
                    author = root["author"]?.jsonPrimitive?.content!!,
                    authorUrl = root["authorUrl"]?.jsonPrimitive?.content,
                    version = root["version"]?.jsonPrimitive?.int!!,
                    isDraft = root["isDraft"]?.jsonPrimitive?.booleanOrNull == true,
                    target = target,
                )

                modules += Module(manifest)
            } catch (e: Exception) {
                mainLog.addLine("Error parsing manifest: ${e.toString()}")
            }
        }
    }

    fun tick() {

    }

    fun mainLoop() {
        Thread {
            while (true) {
                tick()
                Thread.sleep(100)
            }
        }.start()
    }
}