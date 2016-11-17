package com.touchlogger.touch;

import android.util.Log;

import com.touchlogger.capture.CaptureThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Owner on 24.05.2016.
 */
public class EventParser extends TouchEventsSource implements CaptureThread.ICapture {

    private Pattern eventRegex = Pattern.compile("\\[\\s+(\\d+\\.\\d+)\\]\\s+(\\w+)\\s+(\\w+)\\s+([\\w\\d]+)");

    private ArrayList<TouchEvent> currentTouchEvents;
    private ArrayList<ExtendedTouchPoint> lastTouchPoints;
    private ArrayList<TouchEvent> currentTouchEventGroup;
    private int currentTouchEventsSize;
    private int slot;
    private boolean orientationPortrait;
    private Map<String, Integer> trackedGestures;
    private boolean supportsPressure = false;



    private ArrayList<String> lines = new ArrayList<>();
    private ArrayList<String> lastLines;

    private boolean firstEventFound;

    public EventParser() {
        orientationPortrait = true; // dumb default
        setUp();
    }

    private void setUp() {
        currentTouchEvents = new ArrayList<>();
        lastTouchPoints = new ArrayList<>();
        currentTouchEventsSize = 0;
        currentTouchEventGroup = new ArrayList<>();
        trackedGestures = new HashMap<>();

        slot = 0;
        firstEventFound = false;
    }


    public boolean process(String line) {
        try {
            if (line.startsWith("[")) {
                lines.add(line);
                doParseLine(line);
            }
        } catch (Exception e) {
            Log.e("Parser", e.getLocalizedMessage());
            setUp(); // error occurred, setup everything and start over
        }
        return true;
    }

    private void doParseLine(String line) {
        Matcher m = eventRegex.matcher(line);
        m.find();
        if(firstEventFound) {
            switch(m.group(3)) {
                case "ABS_MT_TRACKING_ID":
                    handleTrackingId(m.group(4));
                    break;
                case "ABS_MT_POSITION_X":
                    int x = Integer.parseInt(m.group(4), 16);
                    getCurrentGesture().getLastTouchPoint().x = x;
                    lastTouchPoints.get(slot).x = x;
                    break;
                case "ABS_MT_POSITION_Y":
                    int y = Integer.parseInt(m.group(4), 16);
                    getCurrentGesture().getLastTouchPoint().y = y;
                    lastTouchPoints.get(slot).y = y;
                    break;
                case "ABS_MT_PRESSURE":
                    int pressure = Integer.parseInt(m.group(4), 16);
                    handlePressureValue(pressure);
                    supportsPressure = true;
                    break;
                case "ABS_MT_TOUCH_MAJOR":
                    if(!supportsPressure) {
                        int touch = Integer.parseInt(m.group(4), 16);
                        handleTouchValue(touch);
                        lastTouchPoints.get(slot).touch = touch;
                    }
                    break;
                case "ABS_MT_WIDTH_MAJOR":
                    if(!supportsPressure) {
                        int width = Integer.parseInt(m.group(4), 16);
                        handleWidthValue(width);
                        lastTouchPoints.get(slot).width = width;
                    }
                    break;
                case "SYN_REPORT":
                    endOfTouchPoint();
                    break;
                case "ABS_MT_SLOT":
                    handleSlot(Integer.parseInt(m.group(4), 16));
                case "BTN_TOUCH":
                    if(m.group(4).equals("UP")) {
                        flushGestures();
                    }
            }
        }
        else { // sometimes the first events trackingid or slot are lost, do not evaluate those
            if(m.group(3).equals("ABS_MT_SLOT")) { // track slots
                handleSlot(Integer.parseInt(m.group(4),16));
            }
            if(m.group(3).equals("ABS_MT_TRACKING_ID") && !m.group(4).equals("ffffffff")) { // set tracking id and create next gesture
                handleTrackingId(m.group(4));
                firstEventFound = true;
            }
        }
    }

    private void handleTouchValue(int touch) {
        int width = lastTouchPoints.get(slot).width;
        if(width != 0) {
            int p = touch/width;
            handlePressureValue(p);
        }
    }

    private void handleWidthValue(int width) {
        int touch = lastTouchPoints.get(slot).touch;
        if(touch != 0) {
            int p = touch/width;
            handlePressureValue(p);
        }
    }

    private void handlePressureValue(int p) {
        getCurrentGesture().getLastTouchPoint().pressure = p;
        lastTouchPoints.get(slot).pressure = p;
    }

    private void handleTrackingId(String trackingId) {
        if(trackingId.equals("ffffffff") && getCurrentGesture() != null) {
            getCurrentGesture().setRecording(false);
        } else {
            if(trackedGestures.containsKey(trackingId)) { // already registered tracking id
                slot = trackedGestures.get(trackingId);
            } else { // new tracking id
                while(slot >= currentTouchEvents.size()) { // slot greater than current number of gestures
                    currentTouchEvents.add(null);
                    ExtendedTouchPoint emptyTouchPoint = new ExtendedTouchPoint();
                    lastTouchPoints.add(emptyTouchPoint);
                }
                if (currentTouchEvents.get(slot) != null) {
                    Log.w("EventParser", "New TrackingId " + trackingId + " for nonempty slot " + slot + "/" + getCurrentGesture().getTrackingId() + ". Ignoring new ID.");
                } else {
                    addTouchEvent(trackingId);
                }
            }
        }
    }

    private void addTouchEvent(String trackingId) {
        currentTouchEvents.set(slot, new TouchEvent(true, trackingId, orientationPortrait, lastTouchPoints.get(slot)));
        trackedGestures.put(trackingId, slot);
        currentTouchEventsSize ++;
    }

    private void endOfTouchPoint() {
        if(getCurrentGesture() != null) {
            setTimestampOnCurrent();
            if(getCurrentGesture().isRecording()) {
                addTouchpointOnCurrent();
            } else {
                finishGestures();
            }
        }
    }

    private void handleSlot(int slot) {
        endOfTouchPoint();
        this.slot = slot;
    }

    private void flushGestures() {
        for(int i = 0; i < currentTouchEvents.size(); i ++) {
            TouchEvent touchEvent = currentTouchEvents.get(i);
            if(touchEvent != null) {
                slot = i;
                finishGestures();
            }
        }
        slot = 0;
    }


    private void finishGestures() {
        if(getCurrentGesture().getLastTouchPoint().isEmpty()) {
            getCurrentGesture().removeLastTouchPoint();
        }
        removeCurrentTouchEvent();
    }

    private void removeCurrentTouchEvent() {
        currentTouchEventGroup.add(getCurrentGesture());
        trackedGestures.remove(getCurrentGesture().getTrackingId());
        currentTouchEvents.set(slot, null);
        currentTouchEventsSize --;
        try {
            callbackTouchEventsSinks();
        } catch (Exception e) {
            Log.e("ParserCallback", Log.getStackTraceString(e));
        }
    }

    private void callbackTouchEventsSinks() {
        if(currentTouchEventsSize == 0 && currentTouchEventGroup.size() > 0) {
            sendTouchEvents(currentTouchEventGroup);
            currentTouchEventGroup.clear();
            currentTouchEventsSize = 0;
            lastLines = (ArrayList<String>) lines.clone();
            lines.clear();
        }
    }

    private TouchEvent getCurrentGesture() {
        if(slot >= currentTouchEvents.size()) {
            return null;
        }
        return currentTouchEvents.get(slot);
    }

    private void addTouchpointOnCurrent() {
        if(getCurrentGesture() != null && !getCurrentGesture().getLastTouchPoint().isEmpty()) {
            getCurrentGesture().addNewTouchPoint();
        }
    }

    private void setTimestampOnCurrent() {
        getCurrentGesture().getLastTouchPoint().timestamp = System.currentTimeMillis();
    }

    public void setOrientationPortrait(boolean portrait) {
        this.orientationPortrait = portrait;
    }

    public class ExtendedTouchPoint extends TouchPoint {

        public int touch;
        public int width;

        public ExtendedTouchPoint() {
            super(0,0,0,0);
            touch = 0;
            width = 0;
        }

    }

}
