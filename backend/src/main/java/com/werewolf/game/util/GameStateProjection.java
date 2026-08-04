package com.werewolf.game.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 游戏快照投影：按查看者视角脱敏，只保留该玩家可见的信息。
 * 房主/上帝恢复用完整快照，普通成员只允许读取投影结果。
 */
public final class GameStateProjection {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> PRIVATE_STATE_KEYS = new HashSet<>(Arrays.asList(
            "nightState", "playerVoiceConfigs",
            "miracleMerchantState", "wolfBeautyState", "stalkerState", "silencerState",
            "nightmareState", "magicianState", "cupidState", "wolfBrotherState", "janusState",
            "mechanicalWolfState", "dreamerState", "evilKnightState", "shapeshifterState",
            "cursedFoxState", "showThinking", "typewriterSpeed", "currentViewMode", "selectedPlayerId"
    ));

    private static final Set<String> WOLF_ROLES = new HashSet<>(Arrays.asList(
            "狼人", "狼王", "狼美人", "白狼王", "石像鬼", "机械狼", "狼兄", "狼弟", "百变狼王",
            "Werewolf", "Wolf King", "Wolf Beauty", "White Wolf King", "Gargoyle",
            "Mechanical Wolf", "Wolf Brother", "Wolf Sister", "Shapeshifter Wolf King"
    ));

    private GameStateProjection() {
    }

    /**
     * @param stateJson 完整快照 JSON
     * @param viewerId  观看者玩家 ID；为 null 时所有身份一律隐藏
     * @return 脱敏后的快照 JSON；解析失败返回 null
     */
    public static String project(String stateJson, Long viewerId) {
        try {
            ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(stateJson);
            for (String key : PRIVATE_STATE_KEYS) {
                root.remove(key);
            }
            JsonNode players = root.get("players");
            if (players != null && players.isArray()) {
                boolean viewerIsWolf = viewerId != null && isWolfPlayer(players, viewerId);
                for (JsonNode playerNode : players) {
                    if (!(playerNode instanceof ObjectNode)) {
                        continue;
                    }
                    ObjectNode player = (ObjectNode) playerNode;
                    long playerId = player.path("id").asLong(-1);
                    boolean canSeeRole = viewerId != null && playerId == viewerId;
                    if (viewerIsWolf && isWolfPlayer(players, playerId)) {
                        canSeeRole = true;
                    }
                    if (!canSeeRole) {
                        player.put("role", "unknown");
                    }
                }
            }
            filterPlayerMemories(root, viewerId);
            if (viewerId == null || !isWitchPlayer(players, viewerId)) {
                root.remove("witchInventory");
            }
            JsonNode messages = root.get("dialogMessages");
            if (messages != null && messages.isArray()) {
                ArrayNode publicMessages = OBJECT_MAPPER.createArrayNode();
                for (JsonNode message : messages) {
                    if (!(message instanceof ObjectNode)) {
                        continue;
                    }
                    String visibility = message.path("visibility").asText("public");
                    String type = message.path("type").asText("");
                    long privateFor = message.path("privateFor").asLong(-1);
                    if ("thinking".equals(type)) {
                        continue;
                    }
                    boolean isPublic = "public".equals(visibility);
                    boolean isOwnPrivate = viewerId != null
                            && ("role".equals(visibility) || "private".equals(visibility) || "vote-action".equals(visibility))
                            && privateFor == viewerId;
                    if (!isPublic && !isOwnPrivate) {
                        continue;
                    }
                    ObjectNode copy = ((ObjectNode) message).deepCopy();
                    copy.remove("detail");
                    publicMessages.add(copy);
                }
                root.set("dialogMessages", publicMessages);
            }
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception exception) {
            return null;
        }
    }

    private static void filterPlayerMemories(ObjectNode root, Long viewerId) {
        if (viewerId == null) {
            root.remove("playerMemories");
            return;
        }
        ObjectNode ownMemories = OBJECT_MAPPER.createObjectNode();
        JsonNode memories = root.get("playerMemories");
        if (memories != null && memories.isObject()) {
            JsonNode own = memories.get(String.valueOf(viewerId));
            if (own != null) {
                ownMemories.set(String.valueOf(viewerId), own.deepCopy());
            }
        }
        root.set("playerMemories", ownMemories);
    }

    private static boolean isWitchPlayer(JsonNode players, long playerId) {
        return isRolePlayer(players, playerId, "女巫", "Witch");
    }

    private static boolean isRolePlayer(JsonNode players, long playerId, String... roles) {
        if (players == null || !players.isArray()) {
            return false;
        }
        for (JsonNode playerNode : players) {
            if (playerNode.path("id").asLong(-1) == playerId) {
                String role = playerNode.path("role").asText("");
                for (String candidate : roles) {
                    if (candidate.equals(role)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isWolfPlayer(JsonNode players, long playerId) {
        for (JsonNode playerNode : players) {
            if (playerNode.path("id").asLong(-1) == playerId) {
                return WOLF_ROLES.contains(playerNode.path("role").asText(""));
            }
        }
        return false;
    }
}