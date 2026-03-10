package dev.danielc.common;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import org.jetbrains.annotations.NotNull;

import dev.danielc.libpak.Bluetooth;
import dev.danielc.libpak.Pak;

public class JavaModule extends ModuleInstance {
    public JavaModule(@NotNull ModuleManifest mod) {
        super(mod);
        debugLog("Hello, Java Module");
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onFindConnection(int job) {
        Bluetooth.requestConnectPermission();
        Bluetooth.getDefaultAdapter().isEnabled();
        //debugLog(String.format("adapter name: %s", Bluetooth.adapterName()));

        for (BluetoothDevice e:  Bluetooth.getBondedDevices(Bluetooth.getDefaultAdapter())) {
            debugLog(e.getName());
        }

        Bluetooth.BtFilter filter = new Bluetooth.BtFilter();
        //filter.isClassic = true;

        Bluetooth.pairWithDeviceCompanion(Pak.getActivity(), filter, "device");

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
