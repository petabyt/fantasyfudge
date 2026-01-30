package dev.danielc.common
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

object Runtime {
    var mainLog = ConsoleStateModel()
    var modules: List<Module> = emptyList()
    val moduleInstances: List<ModuleInstance> = emptyList()

    fun loadModulesFromManifests(list: List<String>) {
        for (path in list) {
            val obj: JsonElement = Json.parseToJsonElement(path)
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
                    author = root["description"]?.jsonPrimitive?.content!!,
                    authorUrl = root["description"]?.jsonPrimitive?.content,
                    version = root["version"]?.jsonPrimitive?.int!!,
                    isDraft = root["isDraft"]?.jsonPrimitive?.booleanOrNull == true,
                    target = target,
                )

                modules += Module(manifest)
            } catch (e: Exception) {

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