package com.xxd.vpnspot;

public class ProxyStatusStore {
    private static volatile boolean running;
    private static volatile int port = 8080;
    private static volatile String message = "Stopped";

    public static boolean isRunning() {
        return running;
    }

    public static int getPort() {
        return port;
    }

    public static String getMessage() {
        return message;
    }

    public static void markRunning(int runningPort) {
        running = true;
        port = runningPort;
        message = "Running";
    }

    public static void markStopped() {
        running = false;
        message = "Stopped";
    }

    public static void markError(String errorMessage) {
        running = false;
        message = errorMessage;
    }
}
