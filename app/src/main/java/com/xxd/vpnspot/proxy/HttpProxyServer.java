package com.xxd.vpnspot.proxy;

import com.xxd.vpnspot.ProxyLogStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpProxyServer {
    private static final int MAX_HEADER_BYTES = 64 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 15_000;

    private final int port;
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public HttpProxyServer(int port) {
        this.port = port;
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        running = true;

        acceptThread = new Thread(this::acceptLoop, "vpnspot-proxy-accept");
        acceptThread.start();
    }

    public synchronized void stop() {
        running = false;
        closeQuietly(serverSocket);
        clientExecutor.shutdownNow();
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientExecutor.execute(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        Socket remoteSocket = null;
        try {
            clientSocket.setTcpNoDelay(true);
            InputStream clientIn = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream();

            byte[] headerBytes = readHeader(clientIn);
            String headerText = new String(headerBytes, StandardCharsets.ISO_8859_1);
            ProxyRequest request = ProxyRequestParser.parse(headerText);
            ProxyLogStore.addConnection(request.getMethod(), request.getHost(), request.getPort());

            remoteSocket = new Socket();
            remoteSocket.setTcpNoDelay(true);
            remoteSocket.connect(
                    new InetSocketAddress(request.getHost(), request.getPort()),
                    CONNECT_TIMEOUT_MS);

            if (request.isConnect()) {
                clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n"
                        .getBytes(StandardCharsets.ISO_8859_1));
                clientOut.flush();
            } else {
                OutputStream remoteOut = remoteSocket.getOutputStream();
                remoteOut.write(rewriteHttpHeader(headerText, request)
                        .getBytes(StandardCharsets.ISO_8859_1));
                remoteOut.flush();
            }

            relayBothWays(clientSocket, remoteSocket);
        } catch (Exception e) {
            e.printStackTrace();
            closeQuietly(remoteSocket);
            closeQuietly(clientSocket);
        }
    }

    private byte[] readHeader(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous3 = -1;
        int previous2 = -1;
        int previous1 = -1;
        int current;

        while ((current = inputStream.read()) != -1) {
            buffer.write(current);
            if (buffer.size() > MAX_HEADER_BYTES) {
                throw new IOException("HTTP header too large");
            }

            if (previous3 == '\r'
                    && previous2 == '\n'
                    && previous1 == '\r'
                    && current == '\n') {
                return buffer.toByteArray();
            }

            previous3 = previous2;
            previous2 = previous1;
            previous1 = current;
        }

        throw new IOException("Connection closed before HTTP header completed");
    }

    private String rewriteHttpHeader(String headerText, ProxyRequest request) {
        String[] lines = headerText.split("\\r?\\n");
        StringBuilder rewritten = new StringBuilder();
        rewritten.append(request.toOriginFormRequestLine()).append("\r\n");

        boolean hasHost = false;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                break;
            }

            String lower = line.toLowerCase(Locale.US);
            if (lower.startsWith("proxy-connection:") || lower.startsWith("connection:")) {
                continue;
            }
            if (lower.startsWith("host:")) {
                hasHost = true;
            }
            rewritten.append(line).append("\r\n");
        }

        if (!hasHost) {
            rewritten.append("Host: ").append(request.getHost()).append("\r\n");
        }
        rewritten.append("Connection: close\r\n\r\n");
        return rewritten.toString();
    }

    private void relayBothWays(Socket clientSocket, Socket remoteSocket) throws IOException {
        InputStream clientIn = clientSocket.getInputStream();
        OutputStream clientOut = clientSocket.getOutputStream();
        InputStream remoteIn = remoteSocket.getInputStream();
        OutputStream remoteOut = remoteSocket.getOutputStream();

        clientExecutor.execute(() -> copyAndClose(clientIn, remoteOut, clientSocket, remoteSocket));
        clientExecutor.execute(() -> copyAndClose(remoteIn, clientOut, remoteSocket, clientSocket));
    }

    private void copyAndClose(
            InputStream inputStream,
            OutputStream outputStream,
            Socket firstSocket,
            Socket secondSocket) {
        byte[] buffer = new byte[16 * 1024];
        int read;
        try {
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                outputStream.flush();
            }
        } catch (IOException ignored) {
        } finally {
            closeQuietly(firstSocket);
            closeQuietly(secondSocket);
        }
    }

    private void closeQuietly(ServerSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
