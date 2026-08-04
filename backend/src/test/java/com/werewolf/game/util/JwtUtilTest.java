package com.werewolf.game.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    @Test
    void createAndParseTokenRoundTrip() {
        Long userId = 42L;
        String token = JwtUtil.createToken(userId);
        assertEquals(userId, JwtUtil.parseUserId(token));
    }

    @Test
    void invalidTokenIsRejected() {
        assertThrows(Exception.class, () -> JwtUtil.parseUserId("not-a-jwt"));
    }
}