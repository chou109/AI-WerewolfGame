package com.werewolf.game.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateProjectionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String sampleSnapshot() {
        return "{"
                + "\"version\":3,\"savedAt\":100,"
                + "\"players\":["
                + "{\"id\":1,\"role\":\"狼人\",\"playerName\":\"A\"},"
                + "{\"id\":2,\"role\":\"狼人\",\"playerName\":\"B\"},"
                + "{\"id\":3,\"role\":\"预言家\",\"playerName\":\"C\"},"
                + "{\"id\":4,\"role\":\"女巫\",\"playerName\":\"W\"}"
                + "],"
                + "\"playerMemories\":{"
                + "\"1\":{\"privateKnowledge\":[\"狼队秘密\"]},"
                + "\"3\":{\"privateKnowledge\":[\"查验秘密\"]},"
                + "\"4\":{\"privateKnowledge\":[\"药水秘密\"]}"
                + "},"
                + "\"nightState\":{\"wolfTargetId\":1},"
                + "\"witchInventory\":{\"antidote\":1,\"poison\":0},"
                + "\"dialogMessages\":["
                + "{\"type\":\"speech\",\"visibility\":\"public\",\"content\":\"大家好\"},"
                + "{\"type\":\"referee\",\"visibility\":\"role\",\"privateFor\":1,\"content\":\"你的身份是狼人\"},"
                + "{\"type\":\"referee\",\"visibility\":\"private\",\"privateFor\":3,\"content\":\"你的查验结果：4号狼人\"},"
                + "{\"type\":\"referee\",\"visibility\":\"private\",\"privateFor\":4,\"content\":\"你还有1瓶解药\"},"
                + "{\"type\":\"thinking\",\"visibility\":\"public\",\"content\":\"思考中\"},"
                + "{\"type\":\"referee\",\"visibility\":\"god\",\"content\":\"上帝信息\",\"detail\":\"内部细节\"}"
                + "]"
                + "}";
    }

    private JsonNode read(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    @Test
    void publicProjectionStripsAllPrivateStateAndRoles() throws Exception {
        JsonNode result = read(GameStateProjection.project(sampleSnapshot(), null));

        assertNotNull(result);
        assertFalse(result.has("playerMemories"));
        assertFalse(result.has("nightState"));
        assertFalse(result.has("witchInventory"));
        assertEquals("unknown", result.get("players").get(0).get("role").asText());
        assertEquals("unknown", result.get("players").get(2).get("role").asText());
        JsonNode messages = result.get("dialogMessages");
        assertEquals(1, messages.size());
        assertEquals("大家好", messages.get(0).get("content").asText());
        assertFalse(messages.get(0).has("detail"));
    }

    @Test
    void wolfViewerSeesOwnAndTeammateRolesButNoOthers() throws Exception {
        JsonNode result = read(GameStateProjection.project(sampleSnapshot(), 1L));

        assertEquals("狼人", result.get("players").get(0).get("role").asText());
        assertEquals("狼人", result.get("players").get(1).get("role").asText());
        assertEquals("unknown", result.get("players").get(2).get("role").asText());
        assertEquals("unknown", result.get("players").get(3).get("role").asText());
        // 只保留自己的私密记忆
        JsonNode memories = result.get("playerMemories");
        assertNotNull(memories);
        assertTrue(memories.has("1"));
        assertFalse(memories.has("3"));
        assertFalse(memories.has("4"));
        assertFalse(result.has("witchInventory"));
        // 只看得到公开消息和自己的角色通知
        JsonNode messages = result.get("dialogMessages");
        assertEquals(2, messages.size());
    }

    @Test
    void goodViewerOnlySeesOwnRoleAndOwnPrivateMessages() throws Exception {
        JsonNode result = read(GameStateProjection.project(sampleSnapshot(), 3L));

        assertEquals("unknown", result.get("players").get(0).get("role").asText());
        assertEquals("unknown", result.get("players").get(1).get("role").asText());
        assertEquals("预言家", result.get("players").get(2).get("role").asText());
        assertEquals("unknown", result.get("players").get(3).get("role").asText());
        JsonNode memories = result.get("playerMemories");
        assertNotNull(memories);
        assertFalse(memories.has("1"));
        assertTrue(memories.has("3"));
        assertFalse(memories.has("4"));
        assertFalse(result.has("witchInventory"));
        JsonNode messages = result.get("dialogMessages");
        assertEquals(2, messages.size());
        assertTrue(messages.toString().contains("你的查验结果：4号狼人"));
        assertFalse(messages.toString().contains("你的身份是狼人"));
    }

    @Test
    void witchViewerKeepsOwnInventoryAndPrivateMessages() throws Exception {
        JsonNode result = read(GameStateProjection.project(sampleSnapshot(), 4L));

        assertEquals("unknown", result.get("players").get(0).get("role").asText());
        assertEquals("女巫", result.get("players").get(3).get("role").asText());
        assertTrue(result.has("witchInventory"));
        assertEquals(1, result.get("witchInventory").get("antidote").asInt());
        JsonNode memories = result.get("playerMemories");
        assertNotNull(memories);
        assertFalse(memories.has("1"));
        assertTrue(memories.has("4"));
        JsonNode messages = result.get("dialogMessages");
        assertEquals(2, messages.size());
        assertTrue(messages.toString().contains("你还有1瓶解药"));
    }

    @Test
    void invalidJsonReturnsNull() {
        assertNull(GameStateProjection.project("not-json", null));
    }
}