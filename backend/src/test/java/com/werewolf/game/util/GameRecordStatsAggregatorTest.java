package com.werewolf.game.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameRecordStatsAggregatorTest {

    private static final String GAME_1 = "{\"winner\":\"好人\",\"board\":\"standard\",\"players\":[{\"id\":1,\"number\":1,\"name\":\"雨姐\",\"role\":\"狼人\",\"alive\":false},{\"id\":2,\"number\":2,\"name\":\"丽丽\",\"role\":\"平民\",\"alive\":true},{\"id\":3,\"number\":3,\"name\":\"华强\",\"role\":\"预言家\",\"alive\":true}],\"publicMessages\":[{\"sender\":\"雨姐\",\"content\":\"我是预言家\",\"type\":\"player\"},{\"sender\":\"主持人\",\"content\":\"天亮\",\"type\":\"referee\"}]}";
    private static final String GAME_2 = "{\"winner\":\"狼人\",\"board\":\"miracle_merchant\",\"players\":[{\"id\":4,\"number\":1,\"name\":\"雨姐\",\"role\":\"狼人\",\"alive\":true},{\"id\":5,\"number\":2,\"name\":\"丽丽\",\"role\":\"奇迹商人\",\"alive\":false},{\"id\":6,\"number\":3,\"name\":\"华强\",\"role\":\"平民\",\"alive\":false}],\"publicMessages\":[]}";

    @Test
    void aggregatesPlayerStatsAcrossGames() {
        Map<String, Object> result = GameRecordStatsAggregator.aggregateJson(Arrays.asList(GAME_1, GAME_2));
        assertEquals(2, result.get("totalGames"));
        List<Map<String, Object>> players = (List<Map<String, Object>>) result.get("players");
        Map<String, Map<String, Object>> byName = new java.util.HashMap<>();
        for (Map<String, Object> player : players) {
            byName.put(String.valueOf(player.get("name")), player);
        }

        Map<String, Object> yujie = byName.get("雨姐");
        assertEquals(2, yujie.get("games"));
        assertEquals(1, yujie.get("wins"));
        assertEquals(1, yujie.get("losses"));
        assertEquals(0, yujie.get("draws"));
        assertEquals(50.0, ((Number) yujie.get("winRate")).doubleValue(), 0.01);
        assertEquals(1, yujie.get("speechCount"));
        assertEquals(2, ((Map<?, ?>) yujie.get("roles")).get("狼人"));

        Map<String, Object> lili = byName.get("丽丽");
        assertEquals(1, lili.get("wins"));
        assertEquals(1, lili.get("losses"));
        assertEquals(1, ((Map<?, ?>) lili.get("roles")).get("平民"));
        assertEquals(1, ((Map<?, ?>) lili.get("roles")).get("奇迹商人"));
    }

    @Test
    void aggregatesBoardWinRates() {
        Map<String, Object> result = GameRecordStatsAggregator.aggregateJson(Arrays.asList(GAME_1, GAME_2));
        List<Map<String, Object>> boards = (List<Map<String, Object>>) result.get("boards");
        Map<String, Map<String, Object>> byBoard = new java.util.HashMap<>();
        for (Map<String, Object> board : boards) {
            byBoard.put(String.valueOf(board.get("board")), board);
        }
        assertEquals(1, byBoard.get("standard").get("goodWins"));
        assertEquals(0, byBoard.get("standard").get("wolfWins"));
        assertEquals(1, byBoard.get("miracle_merchant").get("wolfWins"));
        assertEquals(100.0, ((Number) byBoard.get("standard").get("goodWinRate")).doubleValue(), 0.01);
    }

    @Test
    void drawCountsAsDrawAndCursedFoxWinsAlone() {
        Map<String, Object> result = GameRecordStatsAggregator.aggregateJson(Arrays.asList("{\"winner\":\"平局\",\"board\":\"standard\",\"players\":[{\"name\":\"甲\",\"role\":\"平民\"},{\"name\":\"乙\",\"role\":\"狼人\"}],\"publicMessages\":[]}", "{\"winner\":\"咒狐\",\"board\":\"wolfking_guard\",\"players\":[{\"name\":\"丙\",\"role\":\"咒狐\"},{\"name\":\"甲\",\"role\":\"平民\"}],\"publicMessages\":[]}"));
        assertEquals(2, result.get("totalGames"));
        List<Map<String, Object>> players = (List<Map<String, Object>>) result.get("players");
        Map<String, Map<String, Object>> byName = new java.util.HashMap<>();
        for (Map<String, Object> player : players) {
            byName.put(String.valueOf(player.get("name")), player);
        }
        assertEquals(1, byName.get("甲").get("draws"));
        assertEquals(1, byName.get("甲").get("losses"));
        assertEquals(1, byName.get("丙").get("wins"));
        assertEquals(0, byName.get("丙").get("losses"));
    }

    @Test
    void skipsInvalidPayloads() {
        Map<String, Object> result = GameRecordStatsAggregator.aggregateJson(Arrays.asList("{not-json", null, "", GAME_1));
        assertEquals(1, result.get("totalGames"));
    }
}
