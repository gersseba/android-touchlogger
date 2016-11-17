package com.touchlogger.touch;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Owner on 24.05.2016.
 */
public class TouchPoint {
    /** X coordinate of touch event */
    public Integer  x;
    /** Y coordinate of touch event */
    public Integer  y;

    /** pressure of touch event */
    @SerializedName("p")
    public Integer pressure;
    /** Timestamp of touch event */
    @SerializedName("t")
    public Long timestamp;

    public TouchPoint(int x, int y, int p, long timestamp) {
        this.x = x;
        this.y = y;
        this.pressure = p;
        this.timestamp = timestamp;
    }

    public TouchPoint(TouchPoint other) {
        this.x = other.x;
        this.y = other.y;
        this.pressure = other.pressure;
        this.timestamp = other.timestamp;
    }

    public TouchPoint() {

    }

    public boolean isEmpty() {
        return x == null && y == null && pressure == null;
    }

}