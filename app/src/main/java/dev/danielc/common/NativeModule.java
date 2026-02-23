package dev.danielc.common;

import androidx.annotation.NonNull;

import dev.danielc.common.NativeRuntime.*;

public class NativeModule extends ModuleInstance {
    byte[] struct;

    public enum Error {
        PERMISSION_DENIED(-1),
        UNSUPPORTED(-2),
        UNIMPLEMENTED(-3),
        NOT_CONNECTED(-4);

        private final int code;

        Error(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public native int onFindConnection(int job);
    public native int onTryConnectWiFi(@NonNull WiFiAdapter adapter, int job);
    public native int onIdleTick(int usSinceLastTick);
    public native int onDisconnect();
    public native int onSwitchScreen(int oldScreen, int newScreen, int job);
    public native int onRequestFileContents(int screen, int job, FileHandle file);
    public native int onRequestFileThumbnail(int job, FileHandle file);
    public native int onRequestFileMetadata(int job, FileHandle file);
    public native void free();

    public void setScreenSupported(int screen, boolean v) {
        getHomeModelView().addSupportedScreen(screen);
    }

    public void addUserSetting(UserSetting setting) {
        getHomeModelView().addSettingPane(setting);
    }

    public void setProgressBar(NativeRuntime rt, int job) {

    }

    public void isJobCancelled(int job) {

    }

    NativeModule(ModuleManifest manifest) {
        super(manifest);
    }
}
