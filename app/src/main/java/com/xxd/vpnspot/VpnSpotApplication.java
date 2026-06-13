package com.xxd.vpnspot;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Collections;

public class VpnSpotApplication extends AppCompatActivity {
    private static final int TAB_RUN = 0;
    private static final int TAB_LOG = 1;
    private static final long LOG_REFRESH_INTERVAL_MS = 1_000;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable logRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (selectedTab != TAB_LOG) {
                return;
            }

            refreshLogText();
            mainHandler.postDelayed(this, LOG_REFRESH_INTERVAL_MS);
        }
    };

    private int selectedTab = TAB_RUN;
    private Button runTabButton;
    private Button logTabButton;
    private View runPage;
    private View logPage;
    private LinearLayout runContent;
    private LinearLayout logContent;
    private EditText portInput;
    private TextView statusText;
    private TextView addressText;
    private TextView instructionsText;
    private TextView logText;
    private Button clearLogButton;
    private Button toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
        if (selectedTab == TAB_LOG) {
            startLogRefresh();
        }
    }

    @Override
    protected void onPause() {
        stopLogRefresh();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopLogRefresh();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.addView(contentLayout, contentParams());

        TextView titleText = new TextView(this);
        titleText.setText("VPN Spot");
        titleText.setTextSize(28);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(dp(24), dp(32), dp(24), 0);
        contentLayout.addView(titleText, fullWidthParams());

        ScrollView runScrollView = new ScrollView(this);
        runPage = runScrollView;
        contentLayout.addView(runScrollView, contentParams());

        runContent = new LinearLayout(this);
        runContent.setOrientation(LinearLayout.VERTICAL);
        runContent.setGravity(Gravity.CENTER_HORIZONTAL);
        runContent.setPadding(dp(24), 0, dp(24), dp(24));
        runScrollView.addView(runContent, scrollChildParams());

        statusText = new TextView(this);
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        runContent.addView(statusText, spacedParams());

        TextView portLabel = new TextView(this);
        portLabel.setText("Proxy port");
        portLabel.setTextSize(16);
        runContent.addView(portLabel, spacedParams());

        portInput = new EditText(this);
        portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portInput.setSingleLine(true);
        portInput.setText(String.valueOf(ProxyStatusStore.getPort()));
        runContent.addView(portInput, fullWidthParams());

        toggleButton = new Button(this);
        toggleButton.setText("Start");
        toggleButton.setOnClickListener(view -> toggleProxy());
        runContent.addView(toggleButton, spacedParams());

        addressText = new TextView(this);
        addressText.setTextSize(16);
        addressText.setGravity(Gravity.CENTER);
        runContent.addView(addressText, spacedParams());

        instructionsText = new TextView(this);
        instructionsText.setTextSize(15);
        instructionsText.setLineSpacing(0, 1.15f);
        runContent.addView(instructionsText, spacedParams());

        LinearLayout logPageLayout = new LinearLayout(this);
        logPage = logPageLayout;
        logPageLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.addView(logPageLayout, contentParams());

        ScrollView logScrollView = new ScrollView(this);
        logPageLayout.addView(logScrollView, contentParams());

        logContent = new LinearLayout(this);
        logContent.setOrientation(LinearLayout.VERTICAL);
        logContent.setPadding(dp(24), 0, dp(24), dp(16));
        logScrollView.addView(logContent, scrollChildParams());

        logText = new TextView(this);
        logText.setTextSize(15);
        logText.setLineSpacing(0, 1.15f);
        logText.setHorizontallyScrolling(true);

        HorizontalScrollView logHorizontalScrollView = new HorizontalScrollView(this);
        logHorizontalScrollView.setHorizontalScrollBarEnabled(true);
        logHorizontalScrollView.addView(logText, horizontalScrollChildParams());
        logContent.addView(logHorizontalScrollView, spacedParams());

        clearLogButton = new Button(this);
        clearLogButton.setText("Clear Log");
        clearLogButton.setOnClickListener(view -> clearLog());
        LinearLayout.LayoutParams clearButtonParams = fullWidthParams();
        clearButtonParams.setMargins(dp(24), 0, dp(24), dp(8));
        logPageLayout.addView(clearLogButton, clearButtonParams);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(0, 0, 0, 0);
        rootLayout.addView(tabRow, bottomTabRowParams());

        runTabButton = new Button(this);
        runTabButton.setText("首页");
        runTabButton.setAllCaps(false);
        runTabButton.setGravity(Gravity.CENTER);
        runTabButton.setTextSize(11);
        runTabButton.setCompoundDrawablePadding(dp(2));
        runTabButton.setOnClickListener(view -> showTab(TAB_RUN));
        tabRow.addView(runTabButton, tabParams());

        logTabButton = new Button(this);
        logTabButton.setText("数据");
        logTabButton.setAllCaps(false);
        logTabButton.setGravity(Gravity.CENTER);
        logTabButton.setTextSize(11);
        logTabButton.setCompoundDrawablePadding(dp(2));
        logTabButton.setOnClickListener(view -> showTab(TAB_LOG));
        tabRow.addView(logTabButton, tabParams());

        setContentView(rootLayout);
    }

    private void showTab(int tab) {
        selectedTab = tab;
        refreshUi();
        if (selectedTab == TAB_LOG) {
            startLogRefresh();
        } else {
            stopLogRefresh();
        }
    }

    private void toggleProxy() {
        if (ProxyStatusStore.isRunning()) {
            stopProxy();
        } else {
            startProxy();
        }
    }

    private void clearLog() {
        ProxyLogStore.clear();
        refreshUi();
    }

    private void startProxy() {
        Integer port = readPort();
        if (port == null) {
            statusText.setText("Invalid port. Use 1-65535.");
            return;
        }

        ProxyLogStore.clear();
        Intent intent = new Intent(this, ProxyService.class);
        intent.setAction(ProxyService.ACTION_START);
        intent.putExtra(ProxyService.EXTRA_PORT, port);
        ContextCompat.startForegroundService(this, intent);
        ProxyStatusStore.markRunning(port);
        refreshUi();
        mainHandler.postDelayed(this::refreshUi, 600);
    }

    private void stopProxy() {
        Intent intent = new Intent(this, ProxyService.class);
        intent.setAction(ProxyService.ACTION_STOP);
        startService(intent);
        ProxyStatusStore.markStopped();
        refreshUi();
    }

    private Integer readPort() {
        String value = portInput.getText().toString().trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                return null;
            }
            return port;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void refreshUi() {
        boolean running = ProxyStatusStore.isRunning();
        int port = ProxyStatusStore.getPort();
        String host = findLocalIpv4Address();

        statusText.setText("Status: " + ProxyStatusStore.getMessage());
        runPage.setVisibility(selectedTab == TAB_RUN ? View.VISIBLE : View.GONE);
        logPage.setVisibility(selectedTab == TAB_LOG ? View.VISIBLE : View.GONE);
        styleTab(runTabButton, selectedTab == TAB_RUN, R.drawable.ic_tab_home);
        styleTab(logTabButton, selectedTab == TAB_LOG, R.drawable.ic_tab_data);
        portInput.setEnabled(!running);
        toggleButton.setText(running ? "Stop" : "Start");
        addressText.setText("Proxy address: " + host + ":" + port);
        refreshLogText();
        instructionsText.setText(
                "How to connect:\n"
                        + "1. Connect the other device to this phone's hotspot.\n"
                        + "2. Open that device's Wi-Fi proxy settings.\n"
                        + "3. Set proxy host to " + host + " and port to " + port + ".\n"
                        + "4. Keep VPN connected on this phone, then browse from the other device.\n\n"
                        + "This no-root mode works through manual HTTP/HTTPS proxy settings. "
                        + "It is not a transparent VPN tunnel and does not carry UDP traffic.");
    }

    private void refreshLogText() {
        logText.setText(ProxyLogStore.getDisplayText());
    }

    private void startLogRefresh() {
        stopLogRefresh();
        mainHandler.postDelayed(logRefreshRunnable, LOG_REFRESH_INTERVAL_MS);
    }

    private void stopLogRefresh() {
        mainHandler.removeCallbacks(logRefreshRunnable);
    }

    private String findLocalIpv4Address() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                for (java.net.InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "your phone hotspot IP";
    }

    private LinearLayout.LayoutParams fullWidthParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams contentParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f);
    }

    private LinearLayout.LayoutParams spacedParams() {
        LinearLayout.LayoutParams params = fullWidthParams();
        params.topMargin = dp(16);
        return params;
    }

    private LinearLayout.LayoutParams bottomTabRowParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56));
    }

    private ScrollView.LayoutParams scrollChildParams() {
        return new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT);
    }

    private HorizontalScrollView.LayoutParams horizontalScrollChildParams() {
        return new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams tabParams() {
        return new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f);
    }

    private void styleTab(Button button, boolean selected, int iconResId) {
        int contentColor = selected ? Color.WHITE : Color.rgb(80, 80, 80);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(0, dp(4), 0, dp(4));
        button.setTextColor(contentColor);
        button.setBackgroundColor(selected ? Color.rgb(98, 0, 238) : Color.rgb(232, 232, 232));

        Drawable icon = ContextCompat.getDrawable(this, iconResId);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(contentColor);
            int iconSize = dp(28);
            icon.setBounds(0, 0, iconSize, iconSize);
        }
        button.setCompoundDrawables(null, icon, null, null);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
