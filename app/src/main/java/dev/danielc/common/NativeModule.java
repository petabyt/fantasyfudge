package dev.danielc.common;

public class NativeModule {
    SerializableModuleInstance ktConnectionInstance;
    int currentTickInterval;
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

    public static class WiFiAdapter {
        byte[] struct;
    }
    public static class FileHandle {
        byte[] struct;
    }
    public static class FileMetadata {
        String filename;
        String mimeType;
    }

    public void setTickInterval(int us) {
        currentTickInterval = us;
    }

    public native int onFindConnection(int job);
    public native int onTryConnectWiFi(WiFiAdapter adapter, int job);
    public native int onIdleTick(int usSinceLastTick);
    public native int onDisconnect();
    public native int onSwitchScreen(int oldScreen, int newScreen);
    public native int onRequestFileContents(int screen, int job, FileHandle file);
    public native int onRequestFileThumbnail(int job, FileHandle file);
    public native int onRequestFileMetadata(int job, FileHandle file);

    NativeModule(SerializableModuleInstance ktInstance) {
        ktConnectionInstance = ktInstance;
    }
}
