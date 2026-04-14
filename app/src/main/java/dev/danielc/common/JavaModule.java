package dev.danielc.common;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import org.jetbrains.annotations.NotNull;
import dev.danielc.common.screens.GalleryObject;
import dev.danielc.common.screens.SortBy;
import dev.danielc.libpak.Bluetooth;
import dev.danielc.libpak.Pak;

public class JavaModule extends ModuleInstance {
    public JavaModule(@NotNull ModuleManifest mod) {
        super(mod);
        debugLog("Hello, Java Module");
    }

    @SuppressLint("MissingPermission")
    void connectBt() {
        Bluetooth.requestConnectPermission();
        Bluetooth.getDefaultAdapter().isEnabled();
        //debugLog(String.format("adapter name: %s", Bluetooth.adapterName()));

        for (Bluetooth.Device e: Bluetooth.getBondedDevices(Bluetooth.getDefaultAdapter())) {
            debugLog(e.dev.getName());
            debugLog(String.valueOf(e.dev.fetchUuidsWithSdp()));
        }

        Bluetooth.BtFilter filter = new Bluetooth.BtFilter();
        //filter.isClassic = true;

        //Bluetooth.pairWithDeviceCompanion(Pak.getActivity(), filter, "device");
    }

    @Override
    public int onFindConnection(int job) {
        debugLog("Faking connection");
        setScreenSupported(Screen.FILE_GALLERY.getId(), true);

        setStorageInfo(4, "sdcard", SortBy.DEFAULT.getId());

        connectBt();

        return -1;
    }

    int x = 1;

    @Override
    public int onIdleTick(int usSinceLast) {
        Log.d("ASD", "Tick");
        setProperty(ModuleProperty.FIRMWARE_VERSION, String.valueOf(x));
        x++;
        return 0;
    }

    @Override
    public int onTryConnectWiFi(NativeRuntime.@NotNull WiFiAdapter a, int job) {
        return 0;
    }

    @Override
    public int onSwitchScreen(int job, int oldScreen, int newScreen) {
        return 0;
    }

    @Override
    public void free() {

    }

    @Override
    public int onDisconnect() {
        return 0;
    }

    @Override
    public int onRequestFileContents(int job, @NotNull FileHandle file) {
        return 0;
    }

    @Override
    public int onRequestFileThumbnail(int job, @NotNull FileHandle file) {
        return 0;
    }

    @Override
    public int onRequestFileMetadata(int job, @NotNull FileHandle file) {
        return 0;
    }
}
