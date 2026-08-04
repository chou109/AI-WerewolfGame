package com.werewolf.game.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.werewolf.game.entity.AiPlayer;

import java.util.List;

/**
 * AI玩家服务接口
 */
public interface AiPlayerService extends IService<AiPlayer> {
    /**
     * 获取所有可用的AI玩家
     */
    List<AiPlayer> getAvailableAiPlayers();

    /**
     * 根据ID获取AI玩家
     */
    AiPlayer getAiPlayerById(Long id);

    /**
     * 创建AI玩家
     */
    AiPlayer createAiPlayer(AiPlayer aiPlayer);

    /**
     * 更新AI玩家
     */
    boolean updateAiPlayer(AiPlayer aiPlayer);

    /**
     * 获取AI玩家语音配置（不存在返回 null）
     */
    java.util.Map<String, Object> getVoiceConfig(Long id);

    /**
     * 更新AI玩家语音配置
     */
    boolean updateVoiceConfig(Long id, java.util.Map<String, Object> config);

    /**
     * 删除AI玩家
     */
    boolean deleteAiPlayer(Long id);
}
