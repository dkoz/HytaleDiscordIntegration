package com.kozejin;

import java.util.HashMap;
import java.util.Map;

public class AvatarCache {
    private static Map<String, Entry> cache = new HashMap<>();
    private static long expireTimeMs = 1800000;

    private record Entry(String url, long timestamp) {}

    public static synchronized void setExpireTime(int minutes) {
        expireTimeMs = minutes * 60000L;
    }

    public static synchronized String get(String discordId) {
        Entry entry = cache.get(discordId);
        if (entry != null) {
            if (System.currentTimeMillis() - entry.timestamp < expireTimeMs) {
                return entry.url;
            } else {
                cache.remove(discordId);
            }
        }
        return null;
    }

    public static synchronized void put(String discordId, String avatarUrl) {
        cache.put(discordId, new Entry(avatarUrl, System.currentTimeMillis()));
    }

    public static synchronized void clear() {
        cache.clear();
    }

    public static synchronized void cleanExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> 
            now - entry.getValue().timestamp >= expireTimeMs
        );
    }
}
