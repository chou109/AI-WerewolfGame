package com.werewolf.game.service;

import com.werewolf.game.entity.VoiceCache;

/**
 * 云端语音缓存服务
 */
public interface VoiceCacheService {

    /**
     * 按缓存键查找音频
     */
    byte[] findAudio(String cacheKey);

    /**
     * 保存或更新缓存音频
     */
    void saveAudio(String cacheKey, String provider, String voice, String responseFormat, byte[] audio);
}
