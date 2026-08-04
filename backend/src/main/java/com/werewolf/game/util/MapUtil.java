package com.werewolf.game.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Java 8 兼容的 Map 工厂，替代 Map.of（Java 9+）。
 */
public final class MapUtil {

    private MapUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> of(Object... entries) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            map.put((K) entries[i], (V) entries[i + 1]);
        }
        return map;
    }
}
