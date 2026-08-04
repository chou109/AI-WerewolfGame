package com.werewolf.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI玩家实体类
 */
@Data
@TableName("ai_player")
public class AiPlayer implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String modelType;

    private String modelName;

    private String personality;

    private String strategy;

    private String language;

    private String avatarUrl;

    /** AI 语音配置：是否启用、引擎、浏览器音色URI、云端音色、语速/音调/音量 */
    private Integer voiceEnabled;
    private String voiceEngine;
    private String voiceUri;
    private String cloudVoice;
    private Double voiceRate;
    private Double voicePitch;
    private Double voiceVolume;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;

    private String apiBaseUrl;

    private Double temperature;

    private Integer maxTokens;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 仅用于响应展示：是否已配置密钥（真实密钥永不回传）。
     */
    public boolean getHasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * 仅用于响应展示：脱敏后的密钥，如 sk-****abcd。
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }
        String key = apiKey.trim();
        if (key.length() <= 6) {
            return "****";
        }
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }
}
