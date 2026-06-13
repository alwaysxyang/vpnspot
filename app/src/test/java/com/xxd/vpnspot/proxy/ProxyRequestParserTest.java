package com.xxd.vpnspot.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProxyRequestParserTest {
    @Test
    public void parsesConnectRequest() throws Exception {
        ProxyRequest request = ProxyRequestParser.parse(
                "CONNECT example.com:443 HTTP/1.1\r\n"
                        + "Host: example.com:443\r\n"
                        + "\r\n");

        assertTrue(request.isConnect());
        assertEquals("example.com", request.getHost());
        assertEquals(443, request.getPort());
        assertEquals("CONNECT example.com:443 HTTP/1.1", request.getRequestLine());
    }

    @Test
    public void parsesAbsoluteHttpRequest() throws Exception {
        ProxyRequest request = ProxyRequestParser.parse(
                "GET http://example.com:8080/path?q=1 HTTP/1.1\r\n"
                        + "Host: example.com:8080\r\n"
                        + "\r\n");

        assertFalse(request.isConnect());
        assertEquals("example.com", request.getHost());
        assertEquals(8080, request.getPort());
        assertEquals("GET /path?q=1 HTTP/1.1", request.toOriginFormRequestLine());
    }

    @Test
    public void usesDefaultHttpPortWhenMissing() throws Exception {
        ProxyRequest request = ProxyRequestParser.parse(
                "GET http://example.com/path HTTP/1.1\r\n"
                        + "Host: example.com\r\n"
                        + "\r\n");

        assertEquals("example.com", request.getHost());
        assertEquals(80, request.getPort());
        assertEquals("GET /path HTTP/1.1", request.toOriginFormRequestLine());
    }
}
