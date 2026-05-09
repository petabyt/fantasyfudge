package dev.danielc.common;

import androidx.annotation.NonNull;
import dev.danielc.fudge.WiFiAdapter;

public class NativeModule {
    byte[] struct;
    public native int onFindConnection(int job);
    public native int onTryConnectWiFi(@NonNull WiFiAdapter adapter, int job);
    public native int onIdleTick(int usSinceLastTick);
    public native int onDisconnect();
    public native int onSwitchScreen(int oldScreen, int newScreen, int job);
    public native int onRequestFileContents(int job, @NonNull FileHandle file);
    public native int onRequestFileThumbnail(int job, @NonNull FileHandle file);
    public native int onRequestFileMetadata(int job, @NonNull FileHandle file);
    public native void free();
}
