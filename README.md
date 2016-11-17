#  A logger for Touchscreen Interaction on Android

android-touchlogger is a workaround for a security measure [added in Android 4.1](https://github.com/android/platform_frameworks_base/commit/7a9db181a64e0e752a447c6408639bbb33c412fc) that prevents apps from getting touch events when not in the foreground. The usual hack of a transparent overlay no longer works because overlays can either handle the events, but not pass them on, or not handle them.

As no Android API way remains to capture the users' touchscreen interactions, this tool runs its own ADB client over the loopback interface to run the `getevent` command, allowing capture of all interactions regardless of actual screen contents.
In order for this to work, the device must be prepared through USB debugging by running `adb tcpip 6000` to enable network debugging.
The app includes prebuilt ADB and openssl binaries compiled for ARM, built from the AOSP toolchain.
While network debugging somewhat poses a security risk, this is mitigated by the recent use of RSA cryptography in the ADB protocol. Therefore upon first starting the capture, the user needs to accept the app's public key and restart the capture process.

In order to make use of the captured events, implement the `TouchEventSink` or `GestureDetectSink` interface and register them with the corresponding source in the `CaptureThread`.
The source includes a serializer and deserializer, which allows logging and replaying of all touchscreen interaction.
