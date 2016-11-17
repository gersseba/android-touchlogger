package com.touchlogger.capture;

import android.content.Context;
import android.graphics.Point;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.touchlogger.gestures.Gesture;
import com.touchlogger.gestures.GestureDetectSink;
import com.touchlogger.gestures.GestureDetector;
import com.touchlogger.touch.EventParser;
import com.touchlogger.touch.TouchEvent;
import com.touchlogger.touch.TouchEventSerializer;
import com.touchlogger.touch.TouchEventsSink;
import com.touchlogger.touch.TouchEventsSource;
import com.touchlogger.utils.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by Patrick on 23.05.2016.
 */
public class CaptureThread extends Thread {
    private static final String LOGTAG = "CaptureThread";

    public TouchEventProxy proxy = new TouchEventProxy();


    protected CaptureService service;
    protected String device;
    protected boolean cancel = false;
    protected boolean capturing = false;
    protected boolean logging = false;
    protected boolean testing = false;
    protected boolean good = false;
    protected boolean mute = false;

    protected Process currentProcess = null;

    protected EventParser eventParser;
    protected GestureDetector gestureDetector;
    protected TouchEventSerializer serializer;

    private File eventLogDir;
    private boolean logTooLarge;

    protected CaptureParser captureParser;

    public enum Status {
        warning,
        capturing,
        paused
    }

    public class TouchEventProxy extends TouchEventsSource implements TouchEventsSink {

        @Override
        public void onTouchEvents(ArrayList<TouchEvent> touchEvents) {
            sendTouchEvents(touchEvents);
        }
    }

    public CaptureThread(CaptureService srv) {
        service = srv;
        WindowManager wm = (WindowManager) srv.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point dimensions = new Point();
        display.getSize(dimensions);
        gestureDetector = new GestureDetector(dimensions.x, dimensions.y);
        proxy.registerCallback(gestureDetector);
        gestureDetector.registerCallback(new GestureNotifier());
    }

    public void cancelAsync() {
        this.cancel = true;
        synchronized (this) {
            if (this.currentProcess != null) {
                this.currentProcess.destroy();
            }
        }
    }

    public void run() {

        CapturePreparation capturePreparation = new CapturePreparation(service);
        try {
            String adbPath = capturePreparation.getAdbPath();

            printStatus("starting capture");
            startAdb(adbPath);
        } catch (IOException e) {
            printStatus("Error during setup, check log");
            return;
        }
    }




    private int exec(ProcessBuilder processBuilder) throws IOException {
        String output;
        Process process;
        Scanner scanner;

        Log.d(LOGTAG, "executing: " + processBuilder.command());
        process = processBuilder.start();

        int result = -1;
        try {
            result = process.waitFor();
        } catch (InterruptedException e) {
            // should not happen
        }

        scanner = new Scanner(process.getInputStream()).useDelimiter("\\A");
        output = scanner.hasNext() ? scanner.next() : "no output";
        Log.d(LOGTAG, output);
        if (output.contains("unable to connect to")) result = -1;

        return result;
    }

    public void setOrientationPortrait(boolean orientationPortrait) {
        if(eventParser != null) {
            eventParser.setOrientationPortrait(orientationPortrait);
        }
    }

    public interface ICapture {
        /**
         *
         * @param line current line printed from getevent
         * @return whether to continue capturing
         */
        boolean process(String line);
    }

    private int runCapture(ProcessBuilder processBuilder, String filePath, String cmd, String name, ICapture cap) throws IOException {
        String error, output;
        Process process;
        Scanner scanner;
        int result;

//        printStatus("Trying to start " + name);
        processBuilder.command().clear();
        processBuilder.command(filePath, "shell");
        process = processBuilder.start();
        OutputStreamWriter processWriter = new OutputStreamWriter(process.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        int read;
        char[] buffer = new char[4096];

        try {
            Thread.sleep(500);
        } catch(InterruptedException e){}

        // not sure why this works, but this makes it print the prompt right away
        String testCommand = "\r\n";
        processWriter.write(testCommand, 0, testCommand.length());
        try {
            processWriter.flush();
        } catch (IOException e) {
            return handleBrokenPipe(name + " shell", process);
        }

        // read control chars
        boolean hasPrompt = false;
        while(!hasPrompt) {
            read = reader.read(buffer);
            for (int i = 0; i < read; i++) {
                if (buffer[i] == '$') {
                    hasPrompt = true;
                    break;
                }
            }
        }
        printStatus(name + " shell established");

        // now that the prompt is there, write the command
        try {
            processWriter.write(cmd, 0, cmd.length());
            processWriter.flush();
        } catch(IOException e) {
            handleBrokenPipe(name + " getevent", process);
        }

        // read the returned command
        reader.read(buffer);
//        printStatus(name + " command sent: " + cmd);

        synchronized (this) { currentProcess = process;
            capturing = true;
        }
        boolean continuing = true;
        while (continuing) {
            String line = reader.readLine();
            if (line == null) break;
            continuing = cap.process(line);
        }
        synchronized (this) { currentProcess = null; }
        printStatus("Stopped capturing, no more input will be logged or analyzed");

        reader.close();
        process.destroy();
        capturing = false;

        return 0;
    }

    private int handleBrokenPipe(String command, Process process) {
        Scanner scanner;
        String error;
        int result;
        scanner = new Scanner(process.getInputStream()).useDelimiter("\\A");
        error = scanner.hasNext() ? scanner.next() : "no error";
        Log.e(LOGTAG, error);
        printStatus("Reading touch events failed, error: broken pipe");
        try {
            result = process.waitFor();
            return 1;
        } catch (InterruptedException e1) {
            return 1;
        }
    }

    private void printStatus(String status) {
        if(!mute) {
            printStatus(status, Status.warning);
        }
    }

    private void printStatus(String status, Status type) {
        Log.d("CaptureStatus", status);
        service.setNotification(status, type);
    }

    private ProcessBuilder setUpAdbProcess(String filePath) {
        ProcessBuilder processBuilder = new ProcessBuilder(filePath);

        processBuilder.redirectErrorStream(true);

        processBuilder.environment().put("LD_LIBRARY_PATH", service.getApplicationContext().getApplicationInfo().dataDir);
        processBuilder.environment().put("HOME", service.getApplicationContext().getApplicationInfo().dataDir);
        processBuilder.environment().put("ADB_TRACE" , "adb");
        return processBuilder;

    }

    private void startAdb(String filePath) {
        ProcessBuilder processBuilder;
        int result;
        try {
            processBuilder = setUpAdbProcess(filePath);

            killServer(filePath, processBuilder);

            if (startServer(filePath, processBuilder)) {
                // enable buttons
                return;
            }

            if (connectToAdb(filePath, processBuilder)) {
                // enable buttons
                return;
            }


            try {
                Thread.sleep(500);
            } catch(InterruptedException e){}

            printStatus("Please touch screen until this message disappears");
            mute = true;
            result = runCapture(processBuilder, filePath, "getevent -lt\n", "Find device", new ICapture() {
                long nextNotification = 0;
                @Override
                public boolean process(String line) {
//                    Log.d(LOGTAG, line);
                    if (line.startsWith("[")) {
                        long currentMillis = System.currentTimeMillis();
                        if (currentMillis > nextNotification) {
                            printStatus("Finding device in progress, please touch screen until this status disapears");
                            nextNotification = currentMillis + Configuration.notificationInterval;
                        }
                    }
                    if (line.contains("ABS_MT_")) { // multitouch protocol
                        device = line.substring(line.indexOf('/'), line.indexOf(':'));
                        printStatus("Found device:" + device);
                        return false;
                    }
                    return !cancel;
                }
            });
            mute = false;
            captureParser = new CaptureParser();
            result = runCapture(processBuilder, filePath, "getevent -lt " + device + "\n", "Capture", captureParser);

            printStatus("Trying to stop server");
            processBuilder.command(filePath, "kill-server");
            result = exec(processBuilder);
            if (result != 0) {
                printStatus("kill-server failed");
                return;
            }

            printStatus("Stopped capturing, no more input will be logged or analyzed", Status.paused);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private boolean connectToAdb(String filePath, ProcessBuilder processBuilder) throws IOException {
        int result;
        printStatus("Trying to connect adb");
        processBuilder.environment().put("ADB_TRACE" , "");
        processBuilder.command(filePath, "connect", "127.0.0.1:6000");
        result = exec(processBuilder);
        if (result != 0) {
            printStatus("connect adb failed, did you run \"adb tcpip 6000\"?");
            return true;
        }
        return false;
    }

    private boolean startServer(String filePath, ProcessBuilder processBuilder) throws IOException {
        int result;
        printStatus("Trying to start server");
        processBuilder.command(filePath, "start-server");
        result = exec(processBuilder);
        if (result == 0) {

        } else if (result == 255) {
//            printStatus("start-server returned 255, expected");

        } else {
            printStatus("start-server failed: " + result);
            return true;
        }
        return false;
    }

    private void killServer(String filePath, ProcessBuilder processBuilder) throws IOException {
        int result;
        printStatus("Trying to stop server");
        processBuilder.command(filePath, "kill-server");
        result = exec(processBuilder);
        if (result != 0) {
            printStatus("kill-server failed, ignoring");
        }
    }

    class GestureNotifier implements GestureDetectSink {

        @Override
        public void onGestureDetect(Gesture gesture, ArrayList<TouchEvent> events) {
            printStatus("Detected: " + gesture, Status.capturing);
        }
    }

    public File getNextEventLogFile(File dir) throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Configuration.eventLogNameFormat);
        String date = dateFormat.format(new Date());
        File eventFile = new File(dir, date + ".json");
        eventFile.createNewFile();
        return eventFile;
    }

    public File getEventsDirectory(boolean good) {
        File sdCard = Environment.getExternalStorageDirectory();
        if (!sdCard.canWrite()) {
            printStatus("can't write sdcard");
            return null;
        };
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            printStatus("sdcard not mounted");
            return null;
        }
        File dir;
            dir = new File (sdCard.getAbsolutePath());
        dir.mkdirs();
        if (!dir.exists()) {
            printStatus("log dir not created");
            return null;
        }

        return dir;
    }

    public long getSizeOfEventsDirectory() {
        long size = 0;
        for (File file : eventLogDir.listFiles()) {
            if (file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }

    public class CaptureParser implements ICapture {

        private long nextSizeCheck;

        public CaptureParser() {
            eventParser = new EventParser();
            eventParser.registerCallback(proxy);
        }

        @Override
        public boolean process(String line) {
            if (line.startsWith("[")) {
                eventParser.parseLine(line);

                /* todo extract into eventsink
                long currentMillis = System.currentTimeMillis();
                if(logging) {
                    if(currentMillis > nextSizeCheck) {
                        logTooLarge = getSizeOfEventsDirectory() > Configuration.maxEventLogSize;
                        nextSizeCheck = currentMillis + Configuration.sizeCheckInterval;
                    }
                    if(logTooLarge) {
                        try {
                            printStatus("Log files too big, stopped logging.");
                            stopLogging();
                        } catch (IOException e) {
                            Log.e(LOGTAG, "Error while automatically stopping logging " + e.getLocalizedMessage());
                        }
                    }
                }*/

            }
            return !cancel;
        }

        public void finish() throws IOException {
            serializer.finish();
        }
    }
}
