package dev.danielc.common

val temporaryManifestList: List<Module.Manifest> = listOf(
    Module.Manifest(name = "Fujifilm", description = "Connect to Fujifilm cameras", target = Module.Target(deviceId = Module.Device.PROFESSIONAL_CAMERA)),
    Module.Manifest(name = "CMF Nothing", description = "Supports ", target = Module.Target(deviceId = Module.Device.EARBUDS)),
    Module.Manifest(name = "Canon", description = "Canon DSLRs and mirrorless cameras", target = Module.Target(deviceId = Module.Device.PROFESSIONAL_CAMERA)),
    Module.Manifest(name = "Veement", description = "Veement/veecar dashcams", target = Module.Target(deviceId = Module.Device.DASHCAM)),
    Module.Manifest(name = "Toyota", description = "Toyota infotainment system", target = Module.Target(deviceId = Module.Device.AUTOMOTIVE_INFOTAINMENT)),
    Module.Manifest(name = "Roku", description = "Roku TV and media systems", target = Module.Target(deviceId = Module.Device.SMART_TV)),
)