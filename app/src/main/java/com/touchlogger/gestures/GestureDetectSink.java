package com.touchlogger.gestures;

import com.touchlogger.gestures.Gesture;
import com.touchlogger.touch.TouchEvent;

import java.util.ArrayList;

/**
 * Created by Patrick on 08.06.2016.
 */
public interface GestureDetectSink {
    void onGestureDetect(Gesture gesture, ArrayList<TouchEvent> events);
}
