package com.touchlogger.touch;

import java.util.ArrayList;

/**
 * Created by Owner on 08.06.2016.
 */
public class TouchEventCapture {

    protected final int dimX;

    protected final int dimY;

    protected final ArrayList<ArrayList<TouchEvent>> concurrentTouchEvents;

    public TouchEventCapture(int dimX, int dimY, ArrayList<ArrayList<TouchEvent>> concurrentTouchEvents) {
        this.dimX = dimX;
        this.dimY = dimY;
        this.concurrentTouchEvents = concurrentTouchEvents;
    }

}
