package dev.danielc.common;

import androidx.annotation.NonNull;

import dev.danielc.common.NativeRuntime.*;

public class NativeModule extends ModuleInstance {
    byte[] struct;

    public native int onFindConnection(int job);
    public native int onTryConnectWiFi(@NonNull WiFiAdapter adapter, int job);
    public native int onIdleTick(int usSinceLastTick);
    public native int onDisconnect();
    public native int onSwitchScreen(int oldScreen, int newScreen, int job);
    public native int onRequestFileContents(int screen, int job, FileHandle file);
    public native int onRequestFileThumbnail(int job, FileHandle file);
    public native int onRequestFileMetadata(int job, FileHandle file);
    public native void free();

    NativeModule(ModuleManifest manifest) {
        super(manifest);
    }
}
