package com.xxd.vpnspot.proxy;

import java.net.URI;

public class ProxyRequestParser {
    public static ProxyRequest parse(String headerText) throws Exception {
        String[] lines = headerText.split("\\r?\\n");
        if (lines.length == 0 || lines[0].trim().isEmpty()) {
            throw new IllegalArgumentException("Missing request line");
        }

        String requestLine = lines[0];
        String[] parts = requestLine.split(" ", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid request line");
        }

        String method = parts[0];
        String target = parts[1];

        if ("CONNECT".equalsIgnoreCase(method)) {
            HostPort hostPort = parseHostPort(target, 443);
            return new ProxyRequest(method, hostPort.host, hostPort.port, requestLine, target, true);
        }

        URI uri = URI.create(target);
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Missing request host");
        }
        int port = uri.getPort() == -1 ? defaultPort(uri.getScheme()) : uri.getPort();
        String originTarget = buildOriginTarget(uri);
        return new ProxyRequest(method, host, port, requestLine, originTarget, false);
    }

    private static HostPort parseHostPort(String value, int defaultPort) {
        int colonIndex = value.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex == value.length() - 1) {
            return new HostPort(value, defaultPort);
        }
        String host = value.substring(0, colonIndex);
        int port = Integer.parseInt(value.substring(colonIndex + 1));
        return new HostPort(host, port);
    }

    private static int defaultPort(String scheme) {
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return 80;
    }

    private static String buildOriginTarget(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) {
            return path;
        }
        return path + "?" + query;
    }

    private static class HostPort {
        final String host;
        final int port;

        HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
