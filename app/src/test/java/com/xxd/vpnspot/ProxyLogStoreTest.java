package com.xxd.vpnspot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ProxyLogStoreTest {
    @Before
    public void setUp() {
        ProxyLogStore.clear();
    }

    @Test
    public void recordsConnectionAddress() {
        ProxyLogStore.addConnection("CONNECT", "www.baidu.com", 443);

        assertEquals(1, ProxyLogStore.getEntries().size());
        assertTrue(ProxyLogStore.getDisplayText().contains("CONNECT www.baidu.com:443"));
    }

    @Test
    public void clearRemovesEntries() {
        ProxyLogStore.addConnection("GET", "example.com", 80);

        ProxyLogStore.clear();

        assertTrue(ProxyLogStore.getEntries().isEmpty());
        assertEquals("No connections yet.", ProxyLogStore.getDisplayText());
    }
}
