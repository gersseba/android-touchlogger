package com.touchlogger.touch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Owner on 08.06.2016.
 */
public class TouchEventSerializer implements TouchEventsSink {

    private JsonWriter jsonWriter;
    private Gson gson;

    public TouchEventSerializer(File eventFile, int width, int height) throws IOException {
        jsonWriter = new JsonWriter(new FileWriter(eventFile));
        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
        start(width, height);
    }

    private void start(int width, int height) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("dimX").value(width);
        jsonWriter.name("dimY").value(height);
        jsonWriter.name("concurrentTouchEvents").beginArray();
        jsonWriter.flush();
    }

    @Override
    public void onTouchEvents(ArrayList<TouchEvent> touchEvents) {
        try {
            Collections.sort(touchEvents);
            jsonWriter.beginArray();
            for(TouchEvent touchEvent : touchEvents ) {
                jsonWriter.jsonValue(gson.toJson(touchEvent));
            }
            jsonWriter.endArray();
            jsonWriter.flush();
        } catch (IOException e) {
            // real bad
        }
    }

    public void finish() throws IOException {
        jsonWriter.endArray();
        jsonWriter.endObject();
        jsonWriter.flush();
        jsonWriter.close();
    }
}
