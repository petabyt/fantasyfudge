package dev.danielc.common

class DummyModule(manifest: ModuleManifest) : NativeModule(manifest) {
    init {
        NativeRuntime.setupDummyNativeModule(this, manifest)
    }
}