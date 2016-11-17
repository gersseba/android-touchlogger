package com.touchlogger;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.touchlogger.capture.CaptureIntentMessage;
import com.touchlogger.capture.CaptureService;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Map<CaptureIntentMessage, Intent> intents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intents = new HashMap<>();
        for (CaptureIntentMessage message : CaptureIntentMessage.values()) {
            Intent intent = new Intent(this, CaptureService.class);
            intent.setAction(message.name());
            intents.put(message, intent);
        }
//        if(storagePermitted(this)) {
            startService(intents.get(CaptureIntentMessage.START));
//        }

        // todo think about stopping
    }

    private static boolean storagePermitted(Activity activity) {

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return true;

        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1337);
        return false;

    }

}
