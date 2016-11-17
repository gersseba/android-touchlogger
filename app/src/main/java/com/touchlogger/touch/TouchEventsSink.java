package com.touchlogger.touch;

import java.util.ArrayList;

/**
 * Created by Owner on 08.06.2016.
 */
public interface TouchEventsSink {

    void onTouchEvents(ArrayList<TouchEvent> touchEvents);
}
