package dev.danielc.common;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NativeRuntime {
    static Context ctx;
    static native void init();

    public static List<String> getAllJsonManifests() {
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

    public static void setProgressBar(int job) {

    }

    public static void isJobCancelled(int job) {

    }
}
