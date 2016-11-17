package com.touchlogger.touch;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Owner on 08.06.2016.
 */
public class TouchEventDeserializer extends TouchEventsSource {

    protected File[] filesToDeserialize;
    protected Gson gson;

    public TouchEventDeserializer(File... filesToDeserialize) {
        this.filesToDeserialize = filesToDeserialize;
        gson = new Gson();
    }

    public TouchEventDeserializer(File filesDir) {
        this.filesToDeserialize = filesDir.listFiles();
        Arrays.sort(filesToDeserialize);
        gson = new Gson();
    }

    public void deserialize() {
        for(File file : filesToDeserialize) {
            try {
                if(!file.isDirectory() && file.length() > 0) {
                    deserializeFile(file);
                }
            } catch (IOException | JsonSyntaxException e) {
                Log.e("Deserializer", "Error deserializing: " + file.getName());
                attemptFixing(file);
            }
        }
    }

    private void attemptFixing(File file) {
        Log.w("Deserializer", "Trying to fix " + file.getName());
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            fileWriter.write("]}");
            fileWriter.flush();
            fileWriter.close();
            deserializeFile(file);
            Log.w("Deserializer", "Successfully fixed: " + file.getName());
        } catch (IOException eIO) {
            Log.e("Deserializer", "Could not open file for fixing: " + file.getName());
        } catch (JsonSyntaxException eJson) {
            Log.e("Deserializer", "Fix did not help, skipping this file " + file.getName());
        }
    }

    private void deserializeFile(File file) throws IOException {
        TouchEventCapture touchEventCapture = gson.getAdapter(TouchEventCapture.class).fromJson(new FileReader(file));
        for(ArrayList<TouchEvent> concurrentTouchEvent : touchEventCapture.concurrentTouchEvents) {
            sendTouchEvents(concurrentTouchEvent);
        }
    }
}
