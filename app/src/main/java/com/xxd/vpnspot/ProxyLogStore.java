package com.xxd.vpnspot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProxyLogStore {
    private static final int MAX_ENTRIES = 200;
    private static final List<String> entries = new ArrayList<>();

    public static synchronized void addConnection(String method, String host, int port) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        entries.add(0, time + "  " + method + " " + host + ":" + port);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
    }

    public static synchronized List<String> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public static synchronized String getDisplayText() {
        if (entries.isEmpty()) {
            return "No connections yet.";
        }

        StringBuilder builder = new StringBuilder();
        for (String entry : entries) {
            builder.append(entry).append('\n');
        }
        return builder.toString().trim();
    }

    public static synchronized void clear() {
        entries.clear();
    }
}
