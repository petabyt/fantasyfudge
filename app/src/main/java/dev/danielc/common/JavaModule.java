package dev.danielc.common;

import org.jetbrains.annotations.NotNull;

public class JavaModule extends ModuleInstance {
    public JavaModule(@NotNull ModuleManifest mod) {
        super(mod);
        debugLog("Hello, Java Module");
    }

    @Override
    public int onFindConnection(int job) {
        return 0;
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
