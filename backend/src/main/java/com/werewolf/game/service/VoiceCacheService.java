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

    /**
     * 缓存统计（条数、总字节数、最早/最新时间）
     */
    java.util.Map<String, Object> stats();

    /**
     * 清空全部缓存，返回删除条数
     */
    int clearAll();

    /**
     * 清理指定时间之前的缓存，返回删除条数
     */
    int clearOlderThan(long beforeMillis);

    /**
     * 总字节数超过上限时按最旧优先清理，返回删除条数
     */
    int trimToLimit(long maxBytes);
}
