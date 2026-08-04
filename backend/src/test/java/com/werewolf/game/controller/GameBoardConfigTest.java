package com.werewolf.game.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBoardConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void bundledBoardConfigIsCompleteAndConsistent() throws Exception {
        try (InputStream input = new ClassPathResource("configs/board_configs.json").getInputStream()) {
            JsonNode root = MAPPER.readTree(input);
            assertEquals(1, root.path("schemaVersion").asInt());
            JsonNode boards = root.path("boards");
            assertTrue(boards.size() >= 21, "至少包含 21 个板子");

            JsonNode standard = boards.path("standard");
            assertEquals(9, standard.path("players").asInt());
            assertFalse(standard.path("sheriff").asBoolean(), "9人局无警长");
            assertEquals("first-night", standard.path("witchCanSelfSave").asText(), "9人局女巫仅首夜可自救");

            JsonNode miracle = boards.path("miracle_merchant");
            assertTrue(miracle.path("sheriff").asBoolean(), "12人奇迹商人局有警长");
            assertEquals("false", miracle.path("witchCanSelfSave").asText(), "12人局女巫不可自救");

            Iterator<Map.Entry<String, JsonNode>> iterator = boards.fields();
            while (iterator.hasNext()) {
                JsonNode board = iterator.next().getValue();
                int total = 0;
                for (JsonNode role : board.path("roles")) {
                    total += role.path("count").asInt();
                }
                assertEquals(board.path("players").asInt(), total,
                        board.path("key").asText() + " 角色总数必须等于玩家数");
                assertTrue(board.has("nightOrder") && board.path("nightOrder").isArray(),
                        board.path("key").asText() + " 必须包含夜间行动顺序");
                assertTrue(board.has("special") && !board.path("special").asText().isEmpty(),
                        board.path("key").asText() + " 必须包含规则说明");
            }
        }
    }
}