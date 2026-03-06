package dev.danielc.common;

import org.jetbrains.annotations.NotNull;

import dev.danielc.libpak.Bluetooth;
import dev.danielc.libpak.Pak;

public class JavaModule extends ModuleInstance {
    public JavaModule(@NotNull ModuleManifest mod) {
        super(mod);
        debugLog("Hello, Java Module");
    }

    @Override
    public int onFindConnection(int job) {

        Bluetooth.requestConnectPermission();
        //debugLog(String.format("adapter name: %s", Bluetooth.adapterName()));

        Bluetooth.BtFilter filter = new Bluetooth.BtFilter();
        filter.isClassic = false;

        //Bluetooth.pairWithDeviceCompanion(Pak.getActivity(), filter, "device");

        return -1;
    }

    @Override
    public int onTryConnectWiFi(NativeRuntime.@NotNull WiFiAdapter a, int job) {
        return 0;
    }

    @Override
    public int onIdleTick(int usSinceLast) {
        return 0;
    }
}
