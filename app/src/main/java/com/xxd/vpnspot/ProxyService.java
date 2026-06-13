package com.xxd.vpnspot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import com.xxd.vpnspot.proxy.HttpProxyServer;

public class ProxyService extends Service {
    public static final String ACTION_START = "com.xxd.vpnspot.action.START_PROXY";
    public static final String ACTION_STOP = "com.xxd.vpnspot.action.STOP_PROXY";
    public static final String EXTRA_PORT = "port";

    private static final String CHANNEL_ID = "vpnspot_proxy";
    private static final int NOTIFICATION_ID = 1001;

    private HttpProxyServer proxyServer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_STOP : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopProxy();
            return START_NOT_STICKY;
        }

        int port = intent == null ? ProxyStatusStore.getPort() : intent.getIntExtra(EXTRA_PORT, 8080);
        startInForeground(port);
        startProxy(port);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopProxyOnly();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startProxy(int port) {
        try {
            if (proxyServer != null && proxyServer.isRunning() && proxyServer.getPort() == port) {
                ProxyStatusStore.markRunning(port);
                return;
            }

            stopProxyOnly();
            proxyServer = new HttpProxyServer(port);
            proxyServer.start();
            ProxyStatusStore.markRunning(port);
            updateNotification(port);
        } catch (Exception e) {
            stopProxyOnly();
            ProxyStatusStore.markError("Failed to start: " + e.getMessage());
            stopForeground(true);
            stopSelf();
        }
    }

    private void stopProxy() {
        stopProxyOnly();
        ProxyStatusStore.markStopped();
        stopForeground(true);
        stopSelf();
    }

    private void stopProxyOnly() {
        if (proxyServer != null) {
            proxyServer.stop();
            proxyServer = null;
        }
    }

    private void startInForeground(int port) {
        Notification notification = buildNotification(port);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(int port) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, buildNotification(port));
    }

    private Notification buildNotification(int port) {
        Intent launchIntent = new Intent(this, VpnSpotApplication.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_IMMUTABLE
                        : 0);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("VPN Spot proxy")
                .setContentText("Listening on port " + port)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "VPN Spot proxy",
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
}
