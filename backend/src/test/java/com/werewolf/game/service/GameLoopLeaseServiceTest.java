package com.werewolf.game.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameLoopLeaseServiceTest {

    @Test
    void acquireRenewReleaseLifecycle() {
        GameLoopLeaseService service = new GameLoopLeaseService(30000L);
        Map<String, Object> first = service.acquire(1L, "tab-a");
        assertEquals(Boolean.TRUE, first.get("acquired"));

        Map<String, Object> second = service.acquire(1L, "tab-b");
        assertEquals(Boolean.FALSE, second.get("acquired"));
        assertEquals(Boolean.TRUE, second.get("held"));
        assertEquals("tab-a", second.get("holderSession"));

        Map<String, Object> renewOther = service.renew(1L, "tab-b");
        assertEquals(Boolean.FALSE, renewOther.get("acquired"));

        Map<String, Object> renewOwner = service.renew(1L, "tab-a");
        assertEquals(Boolean.TRUE, renewOwner.get("acquired"));

        Map<String, Object> releaseOther = service.release(1L, "tab-b");
        assertEquals(Boolean.FALSE, releaseOther.get("acquired"));

        Map<String, Object> releaseOwner = service.release(1L, "tab-a");
        assertEquals(Boolean.TRUE, releaseOwner.get("acquired"));

        Map<String, Object> afterRelease = service.acquire(1L, "tab-c");
        assertEquals(Boolean.TRUE, afterRelease.get("acquired"));
    }

    @Test
    void expiredLeaseCanBeTakenOver() throws Exception {
        GameLoopLeaseService service = new GameLoopLeaseService(60L);
        service.acquire(2L, "dead-tab");
        Thread.sleep(120L);

        Map<String, Object> takeover = service.acquire(2L, "new-tab");
        assertEquals(Boolean.TRUE, takeover.get("acquired"));

        Map<String, Object> status = service.status(2L);
        assertEquals("new-tab", status.get("holderSession"));
    }
}
