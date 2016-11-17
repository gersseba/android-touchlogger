package com.touchlogger;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

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

//        startService(intents.get(CaptureIntentMessage.START));
    }

    public void startButtonClick(View v) {
        startService(intents.get(CaptureIntentMessage.START));

    }

    public void stopButtonClick(View v) {
        startService(intents.get(CaptureIntentMessage.STOP));

    }
}
