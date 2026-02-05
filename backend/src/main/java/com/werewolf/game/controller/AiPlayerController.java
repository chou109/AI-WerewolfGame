package com.werewolf.game.controller;

import com.werewolf.game.entity.AiPlayer;
import com.werewolf.game.service.AiPlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI玩家控制器
 */
@RestController
@RequestMapping("/ai/player")
public class AiPlayerController {

    @Autowired
    private AiPlayerService aiPlayerService;

    /**
     * 获取所有可用的AI玩家
     */
    @GetMapping("/available")
    public Map<String, Object> getAvailableAiPlayers() {
        List<AiPlayer> aiPlayers = aiPlayerService.getAvailableAiPlayers();
        return Map.of("code", 200, "data", aiPlayers);
    }

    /**
     * 获取AI玩家详情
     */
    @GetMapping("/info/{id}")
    public Map<String, Object> getAiPlayerInfo(@PathVariable Long id) {
        AiPlayer aiPlayer = aiPlayerService.getAiPlayerById(id);
        if (aiPlayer != null) {
            return Map.of("code", 200, "data", aiPlayer);
        } else {
            return Map.of("code", 400, "message", "AI玩家不存在");
        }
    }

    /**
     * 创建AI玩家
     */
    @PostMapping("/create")
    public Map<String, Object> createAiPlayer(@RequestBody AiPlayer aiPlayer) {
        AiPlayer createdAiPlayer = aiPlayerService.createAiPlayer(aiPlayer);
        return Map.of("code", 200, "message", "AI玩家创建成功", "data", createdAiPlayer);
    }

    /**
     * 更新AI玩家
     */
    @PutMapping("/update")
    public Map<String, Object> updateAiPlayer(@RequestBody AiPlayer aiPlayer) {
        boolean result = aiPlayerService.updateAiPlayer(aiPlayer);
        if (result) {
            return Map.of("code", 200, "message", "AI玩家更新成功");
        } else {
            return Map.of("code", 400, "message", "AI玩家更新失败");
        }
    }

    /**
     * 删除AI玩家
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteAiPlayer(@PathVariable Long id) {
        boolean result = aiPlayerService.deleteAiPlayer(id);
        if (result) {
            return Map.of("code", 200, "message", "AI玩家删除成功");
        } else {
            return Map.of("code", 400, "message", "AI玩家删除失败");
        }
    }
}
