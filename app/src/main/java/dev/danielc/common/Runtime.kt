package dev.danielc.common
import dev.danielc.common.screens.ConsoleStateModel

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