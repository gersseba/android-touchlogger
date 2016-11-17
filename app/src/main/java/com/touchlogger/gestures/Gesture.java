package com.touchlogger.gestures;

/**
 * Created by Patrick on 08.06.2016.
 */
public enum Gesture {
    Tap,
    LongPress,
    Scroll,
    Swipe,
    TwoFinger,
    Unidentified;

    public boolean isSupported() {
        return this == Tap || this == LongPress || this == Swipe || this == Scroll;
    }
}
