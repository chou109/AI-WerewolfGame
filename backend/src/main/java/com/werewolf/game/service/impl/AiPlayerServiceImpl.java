package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.werewolf.game.entity.AiPlayer;
import com.werewolf.game.mapper.AiPlayerMapper;
import com.werewolf.game.service.AiPlayerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI玩家服务实现类
 */
@Service
public class AiPlayerServiceImpl extends ServiceImpl<AiPlayerMapper, AiPlayer> implements AiPlayerService {

    @Override
    public List<AiPlayer> getAvailableAiPlayers() {
        return lambdaQuery().eq(AiPlayer::getStatus, 1).list();
    }

    @Override
    public AiPlayer getAiPlayerById(Long id) {
        return getById(id);
    }

    @Override
    public AiPlayer createAiPlayer(AiPlayer aiPlayer) {
        if (aiPlayer.getLanguage() == null || aiPlayer.getLanguage().trim().isEmpty()) {
            aiPlayer.setLanguage("zh-CN");
        }
        if (aiPlayer.getApiKey() != null) {
            aiPlayer.setApiKey(aiPlayer.getApiKey().trim());
        }
        aiPlayer.setCreateTime(LocalDateTime.now());
        aiPlayer.setUpdateTime(LocalDateTime.now());
        aiPlayer.setStatus(1);
        save(aiPlayer);
        return aiPlayer;
    }

    @Override
    public boolean updateAiPlayer(AiPlayer aiPlayer) {
        if (aiPlayer.getLanguage() == null || aiPlayer.getLanguage().trim().isEmpty()) {
            aiPlayer.setLanguage("zh-CN");
        }
        // 密钥留空表示不修改，避免覆盖服务端已保存的密钥
        if (aiPlayer.getApiKey() == null || aiPlayer.getApiKey().trim().isEmpty()) {
            aiPlayer.setApiKey(null);
        } else {
            aiPlayer.setApiKey(aiPlayer.getApiKey().trim());
        }
        aiPlayer.setUpdateTime(LocalDateTime.now());
        return updateById(aiPlayer);
    }

    @Override
    public java.util.Map<String, Object> getVoiceConfig(Long id) {
        AiPlayer player = getById(id);
        if (player == null) {
            return null;
        }
        java.util.Map<String, Object> config = new java.util.LinkedHashMap<>();
        config.put("voiceEnabled", player.getVoiceEnabled() == null ? 1 : player.getVoiceEnabled());
        config.put("voiceEngine", player.getVoiceEngine() == null ? "browser" : player.getVoiceEngine());
        config.put("voiceUri", player.getVoiceUri() == null ? "" : player.getVoiceUri());
        config.put("cloudVoice", player.getCloudVoice() == null ? "alloy" : player.getCloudVoice());
        config.put("voiceRate", player.getVoiceRate() == null ? 1.0 : player.getVoiceRate());
        config.put("voicePitch", player.getVoicePitch() == null ? 1.0 : player.getVoicePitch());
        config.put("voiceVolume", player.getVoiceVolume() == null ? 1.0 : player.getVoiceVolume());
        return config;
    }

    @Override
    public boolean updateVoiceConfig(Long id, java.util.Map<String, Object> config) {
        AiPlayer player = getById(id);
        if (player == null || config == null) {
            return false;
        }
        player.setVoiceEnabled(config.containsKey("voiceEnabled") ? Integer.valueOf(String.valueOf(config.get("voiceEnabled"))) : 1);
        player.setVoiceEngine(config.containsKey("voiceEngine") ? String.valueOf(config.get("voiceEngine")) : "browser");
        player.setVoiceUri(config.containsKey("voiceUri") ? String.valueOf(config.get("voiceUri")) : "");
        player.setCloudVoice(config.containsKey("cloudVoice") ? String.valueOf(config.get("cloudVoice")) : "alloy");
        player.setVoiceRate(config.containsKey("voiceRate") ? Double.valueOf(String.valueOf(config.get("voiceRate"))) : 1.0);
        player.setVoicePitch(config.containsKey("voicePitch") ? Double.valueOf(String.valueOf(config.get("voicePitch"))) : 1.0);
        player.setVoiceVolume(config.containsKey("voiceVolume") ? Double.valueOf(String.valueOf(config.get("voiceVolume"))) : 1.0);
        player.setUpdateTime(LocalDateTime.now());
        return updateById(player);
    }

    @Override
    public boolean deleteAiPlayer(Long id) {
        return removeById(id);
    }

}
