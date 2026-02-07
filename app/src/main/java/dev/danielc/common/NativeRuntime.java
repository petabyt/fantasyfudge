///  Bridge between non-android Kotlin code and native C runtime code
/// (allows kotlin code to be used through compose multiplatform)
package dev.danielc.common;

import android.content.Context;
import android.content.res.AssetManager;
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
    static native NativeModule getDummyModule();

    public static List<String> getAllJsonManifests() {
        Context ctx = weakCtx.get();
        AssetManager assman = ctx.getAssets();
        try {
            List<String> files = new ArrayList<>();
            String[] list = assman.list("");
            if (list == null) return  Collections.emptyList();
            for (String s : list) {
                if (!s.endsWith(".json")) continue;
                InputStream f = assman.open(s);
                byte[] buffer = new byte[f.available()];
                f.read(buffer);
                f.close();
                files.add(new String(buffer));
            }
            return files;
        } catch (Exception e) {
            Log.e("NR", e.toString());
            return Collections.emptyList();
        }
    }

    public static void logGlobalLine(String s) {
        Runtime.INSTANCE.getMainLog().addLine(s);
    }



    public static void setProgressBar(int job) {

    }

    public static void isJobCancelled(int job) {

    }
}
