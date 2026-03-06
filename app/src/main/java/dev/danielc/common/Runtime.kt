package dev.danielc.common
import dev.danielc.R
import dev.danielc.common.screens.ConsoleStateModel
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/// Module defined setting that can be updated by the user or the module
data class UserSetting(
    val name: String,
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
    CONSOLE("console", 100),
    DASHBOARD("dashboard", 101),
    FILE_GALLERY("filegallery", 102),
    FILE_VIEWER("fileviewer", 103),
    GEOTAGGING("geotagging", 104),
    LIVEVIEW("liveview", 105),
    LIVE_FEED("livefeed", 106),
    DISCONNECTED("disconnected", 200);

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
            Screen.NONE -> R.drawable.baseline_question_mark_24
            else -> R.drawable.baseline_question_mark_24
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
            Screen.NONE -> "None"
            else -> "?"
        }
    }
}

typealias JobUpdateCallback = (Job) -> Unit

/// A job id is passed each time a module function is called. A job can be cancelled
/// at any time by the user, and the percent finished value can be updated by the module.
@Serializable
data class Job(
    val onUpdate: JobUpdateCallback,
    val moduleInstance: SerializableModuleInstance,
    val id: Int,
    var progressBarValue: Int = 100,
    var isCancelled: Boolean = false,
    var isFinished: Boolean = false,
)

/// Property IDs for the module instance
enum class ModuleProperty(val id: String) {
    NAME_OF_DEVICE("name"),
    FIRMWARE_VERSION("firmware-version");
    companion object {
        fun fromId(id: String?): ModuleProperty? {
            return ModuleProperty.entries.find { it.id == id }
        }
    }
}

object Runtime {
    var mainLog = ConsoleStateModel()
    var moduleManifests = mutableListOf<ModuleManifest>()
    var moduleInstances = mutableListOf<ModuleInstance>()
    var jobs = mutableListOf<Job>()
    var jobCounter = 0

    fun addModuleInstance(mod: ModuleInstance): Int {
        moduleInstances += mod
        return moduleInstances.lastIndex
    }

    fun removeModuleInstance(mod: ModuleInstance) {
        moduleInstances.remove(mod)
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

    fun closeJob(job: Job) {
        for (e in jobs) {
            if (e == job) {
                jobs.remove(e)
                break
            }
        }
    }

    fun createModuleInstance(m: ModuleManifest): ModuleInstance {
        if (m.moduleType == ModuleManifest.ModuleType.DUMMY_MODULE) {
            return DummyModule(m)
        } else if (m.moduleType == ModuleManifest.ModuleType.JAVA_MODULE) {
            return JavaModule(m)
        } else if (m.moduleType == ModuleManifest.ModuleType.LIBFUJI) {
            return LibFujiModule(m)
        }
        throw Exception("TODO: Implement moduleType")
    }

    fun getManifestFromName(name: String): ModuleManifest? {
        for (m in moduleManifests) {
            if (m.name == name) return m
        }
        return null
    }

    fun refreshManifests() {
        // TODO:
    }

    fun loadModulesFromManifests(list: List<String>) {
        moduleManifests += ModuleManifest(
            name = "Dummy Module",
            description = "Test module that calls some internal C code",
            moduleType = ModuleManifest.ModuleType.DUMMY_MODULE,
            targets = listOf(
                ModuleManifest.Target(
                    company = "Dummy Company",
                    deviceId = Device.GAME_CONTROLLER
                )
            ),
        )

        moduleManifests += ModuleManifest(
            name = "Java Module",
            description = "Test Android APIs",
            moduleType = ModuleManifest.ModuleType.JAVA_MODULE,
            targets = listOf(
                ModuleManifest.Target(
                    company = "Java Company",
                    deviceId = Device.GENERIC_FURNITURE
                )
            ),
        )

        moduleManifests += ModuleManifest(
            name = "libfuji",
            description = "Supports Fujifilm cameras",
            moduleType = ModuleManifest.ModuleType.LIBFUJI,
            targets = listOf(
                ModuleManifest.Target(
                    company = "Fujifilm",
                    deviceId = Device.PROFESSIONAL_CAMERA,
                    products = listOf("X-T1", "X-T2", "X-T3", "X-T4", "X-T5")
                )
            ),
        )

        for (filename in list) {
            try {
                val text = NativeRuntime.readAssetsFile(filename)
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

                moduleManifests += manifest
            } catch (e: Exception) {
                mainLog.addLine("Error parsing manifest: $filename")
                mainLog.addLine(e.toString())
            }
        }
    }
}