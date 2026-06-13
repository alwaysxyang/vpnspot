package com.xxd.vpnspot.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.xxd.vpnspot.ProxyLogStore;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class HttpProxyServerTest {
    @Test
    public void forwardsHttpRequestInOriginForm() throws Exception {
        ProxyLogStore.clear();
        ServerSocket targetServer = new ServerSocket(0);
        int targetPort = targetServer.getLocalPort();
        FutureTask<String> targetRequestLine = new FutureTask<>(() -> {
            try (Socket socket = targetServer.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.ISO_8859_1));
                String requestLine = reader.readLine();
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    // Drain headers.
                }
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(("HTTP/1.1 200 OK\r\n"
                        + "Content-Length: 2\r\n"
                        + "Connection: close\r\n"
                        + "\r\n"
                        + "OK").getBytes(StandardCharsets.ISO_8859_1));
                outputStream.flush();
                return requestLine;
            }
        });
        new Thread(targetRequestLine, "target-http-test").start();

        HttpProxyServer proxyServer = new HttpProxyServer(freePort());
        proxyServer.start();
        try (Socket client = new Socket("127.0.0.1", proxyServer.getPort())) {
            client.getOutputStream().write(("GET http://127.0.0.1:"
                    + targetPort
                    + "/hello HTTP/1.1\r\n"
                    + "Host: 127.0.0.1:"
                    + targetPort
                    + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            client.getOutputStream().flush();

            String response = readAll(client);
            assertTrue(response.contains("200 OK"));
            assertTrue(response.endsWith("OK"));
        } finally {
            proxyServer.stop();
            targetServer.close();
        }

        assertEquals("GET /hello HTTP/1.1", targetRequestLine.get(2, TimeUnit.SECONDS));
        assertTrue(ProxyLogStore.getDisplayText().contains("GET 127.0.0.1:" + targetPort));
    }

    @Test
    public void tunnelsConnectTraffic() throws Exception {
        ServerSocket targetServer = new ServerSocket(0);
        FutureTask<String> payload = new FutureTask<>(() -> {
            try (Socket socket = targetServer.accept()) {
                byte[] buffer = new byte[4];
                int read = socket.getInputStream().read(buffer);
                socket.getOutputStream().write("pong".getBytes(StandardCharsets.ISO_8859_1));
                socket.getOutputStream().flush();
                return new String(buffer, 0, read, StandardCharsets.ISO_8859_1);
            }
        });
        new Thread(payload, "target-connect-test").start();

        HttpProxyServer proxyServer = new HttpProxyServer(freePort());
        proxyServer.start();
        try (Socket client = new Socket("127.0.0.1", proxyServer.getPort())) {
            client.getOutputStream().write(("CONNECT 127.0.0.1:"
                    + targetServer.getLocalPort()
                    + " HTTP/1.1\r\n"
                    + "Host: 127.0.0.1:"
                    + targetServer.getLocalPort()
                    + "\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            client.getOutputStream().flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    client.getInputStream(),
                    StandardCharsets.ISO_8859_1));
            assertTrue(reader.readLine().contains("200"));
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // Drain CONNECT response headers.
            }

            client.getOutputStream().write("ping".getBytes(StandardCharsets.ISO_8859_1));
            client.getOutputStream().flush();

            char[] tunnelResponse = new char[4];
            int read = reader.read(tunnelResponse);
            assertEquals("pong", new String(tunnelResponse, 0, read));
        } finally {
            proxyServer.stop();
            targetServer.close();
        }

        assertEquals("ping", payload.get(2, TimeUnit.SECONDS));
    }

    private int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private String readAll(Socket socket) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(),
                StandardCharsets.ISO_8859_1));
        char[] buffer = new char[128];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }
}
