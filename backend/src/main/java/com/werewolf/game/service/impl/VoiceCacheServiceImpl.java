package com.werewolf.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.werewolf.game.entity.VoiceCache;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.werewolf.game.mapper.VoiceCacheMapper;
import com.werewolf.game.service.VoiceCacheService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 云端语音缓存服务实现
 */
@Service
public class VoiceCacheServiceImpl implements VoiceCacheService {

    @Resource
    private VoiceCacheMapper voiceCacheMapper;

    @Override
    public byte[] findAudio(String cacheKey) {
        VoiceCache cache = voiceCacheMapper.selectOne(
                new LambdaQueryWrapper<VoiceCache>().eq(VoiceCache::getCacheKey, cacheKey));
        return cache == null ? null : cache.getAudio();
    }

    @Override
    public void saveAudio(String cacheKey, String provider, String voice, String responseFormat, byte[] audio) {
        VoiceCache cache = new VoiceCache();
        cache.setCacheKey(cacheKey);
        cache.setProvider(provider);
        cache.setVoice(voice);
        cache.setResponseFormat(responseFormat);
        cache.setAudio(audio);
        cache.setCreatedAt(System.currentTimeMillis());
        VoiceCache existing = voiceCacheMapper.selectById(cacheKey);
        if (existing == null) {
            try {
                voiceCacheMapper.insert(cache);
            } catch (Exception e) {
                // 并发插入冲突时回退为更新
                voiceCacheMapper.updateById(cache);
            }
        } else {
            voiceCacheMapper.updateById(cache);
        }
    }

    @Override
    public java.util.Map<String, Object> stats() {
        java.util.Map<String, Object> stats = voiceCacheMapper.selectStats();
        if (stats == null) {
            stats = new java.util.HashMap<>();
        }
        stats.put("cacheEnabled", true);
        return stats;
    }

    @Override
    public int clearAll() {
        return voiceCacheMapper.delete(Wrappers.<VoiceCache>emptyWrapper());
    }

    @Override
    public int clearOlderThan(long beforeMillis) {
        return voiceCacheMapper.delete(
                new LambdaQueryWrapper<VoiceCache>().lt(VoiceCache::getCreatedAt, beforeMillis));
    }

    @Override
    public int trimToLimit(long maxBytes) {
        if (maxBytes <= 0) {
            return clearAll();
        }
        java.util.List<java.util.Map<String, Object>> entries = voiceCacheMapper.selectLightList();
        long total = 0L;
        for (java.util.Map<String, Object> entry : entries) {
            Object length = entry == null ? null : entry.get("audioLength");
            total += length instanceof Number ? ((Number) length).longValue() : 0L;
        }
        if (total <= maxBytes) {
            return 0;
        }
        int removed = 0;
        for (java.util.Map<String, Object> entry : entries) {
            if (total <= maxBytes) {
                break;
            }
            Object key = entry == null ? null : entry.get("cacheKey");
            Object length = entry == null ? null : entry.get("audioLength");
            if (key == null) {
                continue;
            }
            voiceCacheMapper.deleteById(String.valueOf(key));
            total -= length instanceof Number ? ((Number) length).longValue() : 0L;
            removed++;
        }
        return removed;
    }
}
