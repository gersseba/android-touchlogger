package com.touchlogger.utils;

import com.touchlogger.touch.TouchPoint;

/**
 * Created by Patrick on 17.06.2016.
 */
public class TouchMath {
    public static double angle(int dy, int dx) {
        double angle = Math.toDegrees(Math.atan(dy / (double)dx));
        if (dx < 0) angle = angle + 180;
        angle = (angle + 360) % 360;
        return angle;
    }

    public static double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1,2) + Math.pow(y2 - y1,2));
    }

    public static double distance(TouchPoint from, TouchPoint to) {
        return Math.sqrt(Math.pow(to.x - from.x,2) + Math.pow(to.y - from.y,2));
    }
}
