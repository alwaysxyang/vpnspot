package com.xxd.vpnspot.proxy;

public class ProxyRequest {
    private final String method;
    private final String host;
    private final int port;
    private final String requestLine;
    private final String originTarget;
    private final boolean connect;

    public ProxyRequest(
            String method,
            String host,
            int port,
            String requestLine,
            String originTarget,
            boolean connect) {
        this.method = method;
        this.host = host;
        this.port = port;
        this.requestLine = requestLine;
        this.originTarget = originTarget;
        this.connect = connect;
    }

    public String getMethod() {
        return method;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRequestLine() {
        return requestLine;
    }

    public boolean isConnect() {
        return connect;
    }

    public String toOriginFormRequestLine() {
        String[] parts = requestLine.split(" ", 3);
        if (parts.length != 3) {
            return requestLine;
        }
        return parts[0] + " " + originTarget + " " + parts[2];
    }
}
