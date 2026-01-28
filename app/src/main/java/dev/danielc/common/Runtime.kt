package dev.danielc.common
import kotlinx.serialization.Serializable

// Serializable ID of connection instance that can be passed between activities
@Serializable
data class ConnectionInstance(
    val connectionId: Int?,
) {
    fun getModuleInstance(): ModuleInstance {
        if (connectionId == null) {
            throw Exception();
        } else {
            return Runtime.moduleInstances[connectionId]
        }
    }
}

object Runtime {
    var mainLog = ConsoleStateModel()
    val moduleInstances: List<ModuleInstance> = emptyList()

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