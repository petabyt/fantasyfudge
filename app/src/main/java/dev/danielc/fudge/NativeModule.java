package dev.danielc.fudge;

import androidx.annotation.NonNull;
import java.util.List;

import dev.danielc.common.DashboardPane;
import dev.danielc.common.FileHandle;
import dev.danielc.common.SavedDeviceInfo;
import dev.danielc.libpak.Bluetooth;
import dev.danielc.libpak.WiFi;

public class NativeModule {
    byte[] struct;
    public native int onFindConnection(int job);
    public native int onTryConnectWiFi(@NonNull WiFi.Adapter adapter, int job);
    public native int onTryConnectBluetooth(@NonNull Bluetooth.Device adapter, SavedDeviceInfo saved, int job);
    public native int onIdleTick(int usSinceLastTick);
    public native int onDisconnect();
    public native int onSwitchScreen(int oldScreen, int newScreen, int job);
    public native int onRequestFileContents(int job, @NonNull FileHandle file);
    public native int onRequestFileThumbnail(int job, @NonNull FileHandle file);
    public native int onRequestFileMetadata(int job, @NonNull FileHandle file);
    public native int onRunCommand(int job, String arg0, String arg1, String arg2, String arg3);
    public native int onPropChanged(int job, DashboardPane pane);
    public native void free();
}
