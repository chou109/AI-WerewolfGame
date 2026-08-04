package com.werewolf.game.service;

import com.werewolf.game.util.MapUtil;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏推进租约：同一房间同一时刻只允许一个客户端推进游戏流程，
 * 防止多标签页、重复进入或刷新后同时推进导致重复发言、重复结算。
 * 租约通过心跳续期，超时后自动过期，其他客户端可接管。
 */
@Service
public class GameLoopLeaseService {

    private final long ttlMs;
    private final Map<Long, LeaseEntry> leases = new ConcurrentHashMap<>();

    public GameLoopLeaseService() {
        this(30000L);
    }

    public GameLoopLeaseService(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public synchronized Map<String, Object> acquire(Long roomId, String sessionId) {
        LeaseEntry current = leases.get(roomId);
        long now = System.currentTimeMillis();
        if (current != null && !current.isExpired(now) && !current.sessionId.equals(sessionId)) {
            return result(false, true, current.sessionId, current.expiresIn(now));
        }
        leases.put(roomId, new LeaseEntry(sessionId, now));
        return result(true, false, "", ttlMs);
    }

    public synchronized Map<String, Object> renew(Long roomId, String sessionId) {
        LeaseEntry current = leases.get(roomId);
        long now = System.currentTimeMillis();
        if (current == null || current.isExpired(now)) {
            leases.put(roomId, new LeaseEntry(sessionId, now));
            return result(true, false, "", ttlMs);
        }
        if (!current.sessionId.equals(sessionId)) {
            return result(false, true, current.sessionId, current.expiresIn(now));
        }
        current.lastHeartbeatAt = now;
        return result(true, false, "", ttlMs);
    }

    public synchronized Map<String, Object> release(Long roomId, String sessionId) {
        LeaseEntry current = leases.get(roomId);
        if (current != null && current.sessionId.equals(sessionId)) {
            leases.remove(roomId);
            return result(true, false, "", 0L);
        }
        long now = System.currentTimeMillis();
        boolean held = current != null && !current.isExpired(now);
        return result(false, held, current == null ? "" : current.sessionId,
                current == null ? 0L : current.expiresIn(now));
    }

    public synchronized Map<String, Object> status(Long roomId) {
        LeaseEntry current = leases.get(roomId);
        long now = System.currentTimeMillis();
        if (current == null || current.isExpired(now)) {
            return result(false, false, "", 0L);
        }
        return result(false, true, current.sessionId, current.expiresIn(now));
    }

    private Map<String, Object> result(boolean acquired, boolean held, String holderSession, long expiresInMs) {
        return MapUtil.of(
                "acquired", acquired,
                "held", held,
                "holderSession", holderSession,
                "expiresInMs", Math.max(0L, expiresInMs));
    }

    private final class LeaseEntry {
        private final String sessionId;
        private volatile long lastHeartbeatAt;

        private LeaseEntry(String sessionId, long lastHeartbeatAt) {
            this.sessionId = sessionId;
            this.lastHeartbeatAt = lastHeartbeatAt;
        }

        private boolean isExpired(long now) {
            return now - lastHeartbeatAt > ttlMs;
        }

        private long expiresIn(long now) {
            return ttlMs - (now - lastHeartbeatAt);
        }
    }
}
