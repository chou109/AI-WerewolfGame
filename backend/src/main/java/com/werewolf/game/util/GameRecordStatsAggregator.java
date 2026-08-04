package com.werewolf.game.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 对局记录统计聚合器（纯逻辑，便于单元测试）
 */
public final class GameRecordStatsAggregator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> WOLF_ROLES = new HashSet<>(Arrays.asList(
            "狼人", "狼王", "狼美人", "白狼王", "石像鬼", "梦魇", "恶灵骑士", "机械狼", "狼兄", "狼弟", "百变狼王"
    ));
    private static final Set<String> THIRD_PARTY_ROLES = new HashSet<>(Arrays.asList("咒狐", "千面人"));
    private static final Set<String> DRAW_WINNERS = new HashSet<>(Arrays.asList("平局", "情侣第三方"));

    private GameRecordStatsAggregator() {
    }

    public static Map<String, Object> aggregateJson(List<String> actionContents) {
        List<Map<String, Object>> payloads = new ArrayList<>();
        if (actionContents != null) {
            for (String content : actionContents) {
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                try {
                    Map<String, Object> payload = MAPPER.readValue(content, Map.class);
                    payloads.add(payload);
                } catch (Exception ignored) {
                    // 跳过无法解析的记录
                }
            }
        }
        return aggregate(payloads);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> aggregate(List<Map<String, Object>> payloads) {
        Map<String, PlayerStat> playerStats = new TreeMap<>();
        Map<String, BoardStat> boardStats = new TreeMap<>();
        int totalGames = 0;
        List<Map<String, Object>> sources = payloads == null ? Collections.<Map<String, Object>>emptyList() : payloads;
        for (Map<String, Object> payload : sources) {
            if (payload == null) {
                continue;
            }
            totalGames++;
            String winner = String.valueOf(payload.get("winner"));
            String board = String.valueOf(payload.get("board"));
            if (board == null || "null".equals(board) || board.trim().isEmpty()) {
                board = "unknown";
            }
            BoardStat boardStat = boardStats.computeIfAbsent(board, BoardStat::new);
            if ("好人".equals(winner)) {
                boardStat.goodWins++;
            } else if ("狼人".equals(winner)) {
                boardStat.wolfWins++;
            } else {
                boardStat.other++;
            }
            Object playersRaw = payload.get("players");
            if (playersRaw instanceof List) {
                for (Object raw : (List<?>) playersRaw) {
                    if (!(raw instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> player = (Map<?, ?>) raw;
                    String name = String.valueOf(player.get("name"));
                    if (name == null || "null".equals(name) || name.trim().isEmpty()) {
                        name = "未知";
                    }
                    String role = String.valueOf(player.get("role"));
                    if (role == null || "null".equals(role)) {
                        role = "";
                    }
                    PlayerStat stat = playerStats.computeIfAbsent(name, PlayerStat::new);
                    stat.games++;
                    stat.roleCounts.merge(role, 1, Integer::sum);
                    stat.boardCounts.merge(board, 1, Integer::sum);
                    if (DRAW_WINNERS.contains(winner)) {
                        stat.draws++;
                    } else if ("好人".equals(winner) && "good".equals(sideOf(role))) {
                        stat.wins++;
                    } else if ("狼人".equals(winner) && "wolf".equals(sideOf(role))) {
                        stat.wins++;
                    } else if ("咒狐".equals(winner) && "cursedfox".equals(sideOf(role))) {
                        stat.wins++;
                    } else {
                        stat.losses++;
                    }
                }
            }
            Object messagesRaw = payload.get("publicMessages");
            if (messagesRaw instanceof List) {
                for (Object raw : (List<?>) messagesRaw) {
                    if (!(raw instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> message = (Map<?, ?>) raw;
                    Object type = message.get("type");
                    if (type != null && !"player".equals(String.valueOf(type))) {
                        continue;
                    }
                    String sender = String.valueOf(message.get("sender"));
                    PlayerStat stat = playerStats.get(sender);
                    if (stat != null) {
                        stat.speechCount++;
                    }
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGames", totalGames);
        List<Map<String, Object>> playerList = new ArrayList<>();
        for (PlayerStat stat : playerStats.values()) {
            playerList.add(stat.toMap());
        }
        result.put("players", playerList);
        List<Map<String, Object>> boardList = new ArrayList<>();
        for (BoardStat stat : boardStats.values()) {
            boardList.add(stat.toMap());
        }
        result.put("boards", boardList);
        return result;
    }

    private static String sideOf(String role) {
        if (WOLF_ROLES.contains(role)) {
            return "wolf";
        }
        if ("咒狐".equals(role)) {
            return "cursedfox";
        }
        if (THIRD_PARTY_ROLES.contains(role)) {
            return "third";
        }
        return "good";
    }

    private static final class PlayerStat {
        final String name;
        int games;
        int wins;
        int draws;
        int losses;
        int speechCount;
        final Map<String, Integer> roleCounts = new TreeMap<>();
        final Map<String, Integer> boardCounts = new TreeMap<>();

        PlayerStat(String name) {
            this.name = name;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", name);
            map.put("games", games);
            map.put("wins", wins);
            map.put("draws", draws);
            map.put("losses", losses);
            map.put("winRate", games == 0 ? 0 : Math.round(wins * 1000.0 / games) / 10.0);
            map.put("speechCount", speechCount);
            map.put("roles", new TreeMap<>(roleCounts));
            map.put("boards", new TreeMap<>(boardCounts));
            return map;
        }
    }

    private static final class BoardStat {
        final String board;
        int goodWins;
        int wolfWins;
        int other;

        BoardStat(String board) {
            this.board = board;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("board", board);
            int total = goodWins + wolfWins + other;
            map.put("games", total);
            map.put("goodWins", goodWins);
            map.put("wolfWins", wolfWins);
            map.put("other", other);
            map.put("goodWinRate", total == 0 ? 0 : Math.round(goodWins * 1000.0 / total) / 10.0);
            return map;
        }
    }
}
