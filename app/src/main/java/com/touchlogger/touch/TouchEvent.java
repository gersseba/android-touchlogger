package com.touchlogger.touch;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by Owner on 24.05.2016.
 */
public class TouchEvent implements Comparable<TouchEvent> {

    private transient boolean recording;
    private transient String trackingId;

    @SerializedName("Ts")
    private ArrayList<TouchPoint> touchPoints = new ArrayList<>();

    @SerializedName("o")
    private boolean orientationPortrait;

    public TouchEvent() {
        super();
        initTouchPoints();
    }

    public TouchEvent(boolean recording, String trackingId, boolean orientationPortrait, TouchPoint touchPoint) {
        super();
        this.recording = recording;
        this.trackingId = trackingId;
        this.orientationPortrait = orientationPortrait;
        initTouchPoints(touchPoint);
    }

    public void initTouchPoints() {
        touchPoints = new ArrayList<>();
        touchPoints.add(new TouchPoint());
    }

    public void initTouchPoints(TouchPoint touchPoint) {
        touchPoints = new ArrayList<>();
        touchPoints.add(new TouchPoint(touchPoint));
    }

    public ArrayList<TouchPoint> getTouchPoints() {
        return touchPoints;
    }

    public TouchPoint getLastTouchPoint() {
        return touchPoints.get(touchPoints.size() - 1);
    }

    public TouchPoint getFirstTouchPoint() {
        return touchPoints.get(0);
    }

    public void removeLastTouchPoint() {
        touchPoints.remove(touchPoints.size() - 1);
    }

    public void addNewTouchPoint() {
        touchPoints.add(new TouchPoint());
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setOrientationPortrait(boolean portrait) {
        orientationPortrait = portrait;
    }

    public boolean isOrientationPortrait() {
        return orientationPortrait;
    }

    public void fillSparseData() {
        TouchPoint last = touchPoints.get(0);
        for (TouchPoint point : touchPoints) {
            if (point.x == null) {
                point.x = last.x;
            }
            if (point.y == null) {
                point.y = last.y;
            }
            if (point.pressure == null) {
                point.pressure = last.pressure;
            }
            last = point;
        }
    }

    @Override
    public int compareTo(TouchEvent another) {
        return (int)(this.getFirstTouchPoint().timestamp - ((TouchEvent) another).getFirstTouchPoint().timestamp);
    }
}
