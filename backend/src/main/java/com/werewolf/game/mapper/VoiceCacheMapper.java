package com.werewolf.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.werewolf.game.entity.VoiceCache;

/**
 * 语音缓存Mapper接口
 */
public interface VoiceCacheMapper extends BaseMapper<VoiceCache> {

    /**
     * 统计缓存条数、总字节数与最早/最新时间
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) AS cacheCount, COALESCE(SUM(LENGTH(audio)), 0) AS totalBytes, MIN(created_at) AS oldestAt, MAX(created_at) AS newestAt FROM voice_cache")
    java.util.Map<String, Object> selectStats();

    /**
     * 按创建时间升序返回轻量缓存清单（不含音频内容），用于超限清理
     */
    @org.apache.ibatis.annotations.Select("SELECT cache_key AS cacheKey, LENGTH(audio) AS audioLength, created_at AS createdAt FROM voice_cache ORDER BY created_at ASC")
    java.util.List<java.util.Map<String, Object>> selectLightList();
}
