package com.werewolf.game.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 云端语音缓存实体
 */
@Data
@TableName("voice_cache")
public class VoiceCache implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String cacheKey;

    private String provider;

    private String voice;

    private String responseFormat;

    private byte[] audio;

    private Long createdAt;
}
