package com.touchlogger.gestures;

import android.util.Log;

import com.touchlogger.touch.TouchEvent;
import com.touchlogger.touch.TouchEventsSink;
import com.touchlogger.touch.TouchPoint;
import com.touchlogger.utils.TouchMath;

import java.util.ArrayList;

/**
 * Created by Patrick on 08.06.2016.
 */
public class GestureDetector extends GestureDetectSource implements TouchEventsSink {
    final static String LOGTAG = "GestureDetector";
    final static int MAX_TAP_DURATION = 500;
    final static double MAX_TAP_DISTANCE_FACTOR = 1.0/25.0;
    final static double MIN_SWIPE_DISTANCE_FACTOR = 0.1;
    final static double SWIPE_ANGLE_THRESHOLD = 15;
    final static double SCROLL_CLUSTER_DISTANCE = 5;
    double maxTapDistance;
    double minSwipeDistance;
    int screenX;
    int screenY;

    public GestureDetector(int screenX, int screenY) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.maxTapDistance = screenX * MAX_TAP_DISTANCE_FACTOR;
        this.minSwipeDistance = screenX * MIN_SWIPE_DISTANCE_FACTOR;
    }

    private boolean detectTapOrPress(TouchEvent event) {
        TouchPoint first = event.getFirstTouchPoint();
        int lastX = first.x;
        int lastY = first.y;
        for (TouchPoint point : event.getTouchPoints()) {
            if (point.x == null && point.y == null)
                continue;

            int x = point.x != null ? point.x : lastX;
            int y = point.y != null ? point.y : lastY;
            if (TouchMath.distance(x, y, lastX, lastY) > maxTapDistance) {
                return false;
            }
            if (TouchMath.distance(x, y, first.x, first.y) > maxTapDistance) {
                return false;
            }
            lastX = x;
            lastY = y;
        }

        if (event.getLastTouchPoint().timestamp - event.getFirstTouchPoint().timestamp <= MAX_TAP_DURATION) {
            sendGestureDetect(Gesture.Tap, event);
            return true;
        } else {
            sendGestureDetect(Gesture.LongPress, event);
            return true;
        }
    }


    private boolean detectScrollOrSwipe(TouchEvent event) {
        // Assumption: It's not a tap because that was checked before
        TouchPoint first = event.getFirstTouchPoint();

        int lastX = first.x;
        int lastY = first.y;
        double lastAngle = Double.MIN_VALUE;
        boolean beginClustering = true;
        boolean endClustering = false;
        for (TouchPoint point : event.getTouchPoints()) {
            if (point.x == null && point.y == null)
                continue;

            int x = point.x != null ? point.x : lastX;
            int y = point.y != null ? point.y : lastY;

            int dx = x - lastX;
            int dy = y - lastY;

            if (dx != 0 || dy != 0) {
                if (TouchMath.distance(x, y, lastX, lastY) > SCROLL_CLUSTER_DISTANCE) {
                    beginClustering = false;
                    if (endClustering) {
                        return false;
                    }

                    double angle = TouchMath.angle(dy, dx);
                    if (lastAngle != Double.MIN_VALUE) {
                        double diff = Math.abs(angle - lastAngle);
                        if (diff > 180) diff = 360 - diff;
                        if (diff > SWIPE_ANGLE_THRESHOLD) {
                            return false;
                        }
                    }

                    lastAngle = angle;
                } else {
                    if (!beginClustering) {
                        endClustering = true;
                    }
                }

            }

            lastX = x;
            lastY = y;
        }
        if (TouchMath.distance(first.x, first.y, lastX, lastY) < minSwipeDistance) {
            return false;
        }

        if (!endClustering) {
            try {
                sendGestureDetect(Gesture.Swipe, event);
            } catch (Exception e) {
                Log.d("Gesture Detector", "Error detecting swipe");
            }
        } else {
            sendGestureDetect(Gesture.Scroll, event);
        }
        return true;
    }

    @Override
    public void onTouchEvents(ArrayList<TouchEvent> touchEvents) {
        if (touchEvents.size() > 2) {
            sendGestureDetect(Gesture.Unidentified, touchEvents);
            return;
        }
        if (touchEvents.size() == 2) {
            sendGestureDetect(Gesture.TwoFinger, touchEvents);
            return;
        }

        TouchEvent event = touchEvents.get(0);


        if (!cleanEvent(event)) {
            return;
        }

        if (detectTapOrPress(event)) {
            return;
        }

        if (detectScrollOrSwipe(event)) {
            return;
        }

        sendGestureDetect(Gesture.Unidentified, touchEvents);
    }

    private boolean cleanEvent(TouchEvent event) {
        if (event.getFirstTouchPoint() == null) {
            Log.w(LOGTAG, "Event has no first touch point, skipping");
            return false;
        }

        if (event.getFirstTouchPoint().timestamp == null) {
            Log.w(LOGTAG, "First touch point has no timestamp, skipping");
            return false;
        }

        if (event.getFirstTouchPoint().x == null || event.getFirstTouchPoint().y == null) {
            Log.w(LOGTAG, "First touch point has missing coordinates, skipping");
            return false;
        }

        if (event.getFirstTouchPoint().pressure == null) {
            Log.w(LOGTAG, "First touch point has no pressure, skipping");
            return false;
        }


        while (event.getLastTouchPoint() != null && event.getLastTouchPoint().isEmpty()) {
            event.removeLastTouchPoint();
        }
        if (event.getLastTouchPoint() == null) {
            Log.w(LOGTAG, "Event has no touch points left after clearing empty, skipping");
            return false;
        }
        if (event.getLastTouchPoint().timestamp == null) {
            Log.w(LOGTAG, "Last touch point has no timestamp, skipping");
            return false;
        }

        event.fillSparseData();

        return true;
    }
}
