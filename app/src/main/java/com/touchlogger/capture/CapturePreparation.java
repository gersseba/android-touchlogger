package com.touchlogger.capture;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Owner on 03.06.2016.
 */
public class CapturePreparation {

    private static final String LOGTAG = "CapturePreparation";
    private CaptureService service;

    public CapturePreparation(CaptureService service) {
        this.service = service;
    }

    public String getAdbPath() throws IOException {
        String dataFolder = service.getApplicationContext().getApplicationInfo().dataDir;
        copyFile("adb", dataFolder, "adb");
        copyFile("adb", dataFolder, "libcrypto.so");
        copyFile("adb", dataFolder, "libssl.so");
        return dataFolder + "/adb";
    }

    private void copyFile(String assetDir, String localDir, String filename) throws IOException {
        File outFile = new File(localDir + "/" + filename);
        if(outFile.exists()) {
            Log.w(LOGTAG, filename + " already exists");
            return;
        }
        Log.d(LOGTAG, "copying " + filename);
        InputStream in = service.getApplicationContext().getAssets().open(assetDir + "/" + filename);
        FileOutputStream out = new FileOutputStream(outFile);
        int read;
        byte[] buffer = new byte[4096];
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        out.close();
        in.close();
        outFile.setExecutable(true);
        Log.d(LOGTAG, "finished copying " + filename);
    }
}
