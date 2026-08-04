package com.werewolf.game.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPlayerMaskingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void apiKeyIsNeverSerialized() throws Exception {
        AiPlayer player = new AiPlayer();
        player.setId(1L);
        player.setName("TestAI");
        player.setApiKey("sk-abcdefghijkl1234");

        String json = mapper.writeValueAsString(player);

        assertFalse(json.contains("apiKey"));
        assertFalse(json.contains("sk-abcdefghijkl1234"));
        assertTrue(json.contains("\"hasApiKey\":true"));
        assertTrue(json.contains("\"maskedApiKey\":\"sk-****1234\""));
    }

    @Test
    void emptyApiKeyProducesNullMask() {
        AiPlayer player = new AiPlayer();
        player.setApiKey("   ");
        assertFalse(player.getHasApiKey());
        assertNull(player.getMaskedApiKey());
    }

    @Test
    void shortApiKeyIsFullyMasked() {
        AiPlayer player = new AiPlayer();
        player.setApiKey("abc");
        assertEquals("****", player.getMaskedApiKey());
    }

    @Test
    void longApiKeyKeepsOnlyEdges() {
        AiPlayer player = new AiPlayer();
        player.setApiKey("sk-abcdefghijkl1234");
        assertEquals("sk-****1234", player.getMaskedApiKey());
    }
}