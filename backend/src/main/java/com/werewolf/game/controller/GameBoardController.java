package com.werewolf.game.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.werewolf.game.util.MapUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 板子规则配置接口：以 classpath 下的 board_configs.json 为唯一数据源。
 * 与前端 frontend/src/game/board_configs.json 保持同步。
 */
@RestController
@RequestMapping("/game/board")
public class GameBoardController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CONFIG_PATH = "configs/board_configs.json";

    private final AtomicReference<JsonNode> configCache = new AtomicReference<>();

    @GetMapping("/version")
    public Map<String, Object> getVersion() throws Exception {
        JsonNode root = loadConfig();
        return MapUtil.of("code", 200, "data", MapUtil.of(
                "schemaVersion", root.path("schemaVersion").asInt(1),
                "boardCount", root.path("boards").size()
        ));
    }

    @GetMapping("/configs")
    public Map<String, Object> getConfigs() throws Exception {
        return MapUtil.of("code", 200, "data", loadConfig());
    }

    @GetMapping("/{key}")
    public Map<String, Object> getBoard(@PathVariable String key) throws Exception {
        JsonNode root = loadConfig();
        JsonNode board = root.path("boards").path(key);
        if (board.isMissingNode() || !board.isObject()) {
            return MapUtil.of("code", 404, "message", "板子配置不存在: " + key);
        }
        return MapUtil.of("code", 200, "data", MapUtil.of(
                "schemaVersion", root.path("schemaVersion").asInt(1),
                "board", board
        ));
    }

    private JsonNode loadConfig() throws Exception {
        JsonNode cached = configCache.get();
        if (cached != null) {
            return cached;
        }
        ClassPathResource resource = new ClassPathResource(CONFIG_PATH);
        try (InputStream input = resource.getInputStream()) {
            JsonNode parsed = MAPPER.readTree(input);
            configCache.compareAndSet(null, parsed);
            return parsed;
        }
    }
}