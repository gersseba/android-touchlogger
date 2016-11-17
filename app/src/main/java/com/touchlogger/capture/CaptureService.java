package com.touchlogger.capture;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

import com.touchlogger.MainActivity;
import com.touchlogger.R;

import static com.touchlogger.capture.CaptureThread.Status.capturing;

public class CaptureService extends Service {

    private static final String BCAST_CONFIGCHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    private static final String LOGTAG = "CaptureService";
    private static final int WAITING_TIME_FOR_CANCEL = 10000;

    CaptureThread thread = null;

    public CaptureService() {
    }

    public void onCreate() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BCAST_CONFIGCHANGED);
        this.registerReceiver(mBroadcastReceiver, filter);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGTAG, "Service received intent: " + intent.getAction());
        CaptureIntentMessage message = CaptureIntentMessage.valueOf(intent.getAction());
        switch(message) {
            case START:
                if(thread != null && thread.getState() != Thread.State.TERMINATED) {
                    if(!thread.cancel) {
                        Log.w(LOGTAG, "Ignoring start request, already running");
                        // enable buttons
                        return START_REDELIVER_INTENT; // already running, ignore
                    }
                    if(!cancelThreadAndWait()) {
                        Log.e(LOGTAG, "Thread could not be cancelled");
                        // enable buttons
                        return START_REDELIVER_INTENT;
                    }
                }
                thread = new CaptureThread(this);
                thread.start();
                break;
            case STOP:
                if (thread != null) {
                    thread.cancelAsync();
                }
                break;
        }
        return START_REDELIVER_INTENT;
    }

    public boolean isThreadRunning() {
        return thread != null && thread.getState() != Thread.State.TERMINATED && !thread.cancel ;
    }

    public boolean cancelThreadAndWait() {
        thread.cancelAsync();
        try {
            synchronized (this) {
                this.wait(WAITING_TIME_FOR_CANCEL);
            }
        } catch (InterruptedException e) {

        }
        return thread == null || thread.getState() == Thread.State.TERMINATED;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent myIntent) {

            if ( myIntent.getAction().equals( BCAST_CONFIGCHANGED ) && thread != null && thread.getState() != Thread.State.TERMINATED ) {
                thread.setOrientationPortrait(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
            }
        }
    };

    public void setNotification(String status, CaptureThread.Status type) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent clickIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification.Builder b = new Notification.Builder(this)
                .setContentTitle("Touch Capturing")
                .setContentText(status)
                .setContentIntent(clickIntent)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX);

        switch (type) {
            case capturing:
                b.setSmallIcon(R.mipmap.ic_notification);
                break;
            case paused:
                b.setSmallIcon(R.mipmap.ic_notification_inactive);
                break;
            case warning:
            default:
                b.setSmallIcon(R.mipmap.ic_notification_err);
                break;
        }
        if (type == capturing) {
            // This is supposed to prevent us from being killed (if low on memory?)
            this.startForeground(42, b.build());
        } else {
            this.stopForeground(false);
            NotificationManager notificationManager= (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(42, b.build());
        }
    }
}
