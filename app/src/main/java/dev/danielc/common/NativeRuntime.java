///  Bridge between non-android Kotlin code and native C runtime code
/// (allows kotlin code to be used through compose multiplatform)
package dev.danielc.common;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Network;
import android.util.Log;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NativeRuntime {
    public static boolean hasInited = false;
    public static WeakReference<Context> weakCtx = null;
    public static void setupAndroidContext(Context ctx) {
        weakCtx = new WeakReference<>(ctx);
    }
    static native void init();
    static native NativeModule getDummyModule(ModuleManifest manifest);
    static native int setupDummyNativeModule(NativeModule mod, ModuleManifest manifest);
    static native int setupJavascriptModule(NativeModule mod, ModuleManifest manifest, String jsPath);
    static native int getWebassemblyModule(NativeModule mod, ModuleManifest manifest, String jsPath);

    public static class WiFiAdapter {
        Network net;
    }
    public static class FileHandle {
        int index;
        String filename;
    }
    public static class FileMetadata {
        String filename;
        String mimeType;
    }

    public static List<String> getJsonManifestList() {
        Context ctx = weakCtx.get();
        AssetManager assman = ctx.getAssets();
        try {
            List<String> files = new ArrayList<>();
            String[] list = assman.list("");
            if (list == null) return Collections.emptyList();
            for (String s : list) {
                if (!s.endsWith(".json")) continue;
                files.add(s);
            }
            return files;
        } catch (Exception e) {
            Log.e("NR", e.toString());
            return Collections.emptyList();
        }
    }

    public static byte[] readAssetsFile(String path) throws Exception {
        Context ctx = weakCtx.get();
        AssetManager assman = ctx.getAssets();
        InputStream f = assman.open(path);
        byte[] buffer = new byte[f.available()];
        int n = f.read(buffer);
        f.close();
        return buffer;
    }

    public static void logGlobalLine(String s) {
        Runtime.INSTANCE.getMainLog().addLine(s);
    }
}
