package dev.danielc.common;

public class NativeModule {
    public static class WiFiAdapter {
        byte[] struct;
    }
    public static class FileHandle {
        byte[] struct;
    }
    public native int onFindConnection(int job);
    public native int onTryConnectWiFi(WiFiAdapter adapter, int job);
    public native int onIdleTick(int usSinceLastTick);
    public native int onDisconnect();
    public native int onSwitchScreen(int oldScreen, int newScreen);
    public native int onRequestFileContents(int screen, int job, FileHandle file);
    public native int onRequestFileThumbnail(int job, FileHandle file);
    public native int onRequestFileMetadata(int job, FileHandle file);
}
