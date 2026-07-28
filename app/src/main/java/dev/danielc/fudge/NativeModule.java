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
    public synchronized native int onFindConnection(int job);
    public synchronized native int onTryConnectWiFi(@NonNull WiFi.Adapter adapter, int job);
    public synchronized native int onTryConnectBluetooth(@NonNull Bluetooth.Device adapter, SavedDeviceInfo saved, int job);
    public synchronized native int onIdleTick(int usSinceLastTick);
    public synchronized native int onDisconnect();
    public synchronized native int onSwitchScreen(int oldScreen, int newScreen, int job);
    public synchronized native int onRequestFileContents(int job, @NonNull FileHandle file);
    public synchronized native int onRequestFileThumbnail(int job, @NonNull FileHandle file);
    public synchronized native int onRequestFileMetadata(int job, @NonNull FileHandle file);
    public synchronized native int onRunCommand(int job, String arg0, String arg1, String arg2, String arg3);
    public synchronized native int onPropChanged(int job, DashboardPane pane);
    public synchronized native void free();
    public synchronized native void setSetupOptionName(String name);
}
