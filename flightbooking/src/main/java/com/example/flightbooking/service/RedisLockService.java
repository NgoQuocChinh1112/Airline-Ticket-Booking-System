package com.example.flightbooking.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    // Lua script: chỉ xóa lock nếu đúng chủ sở hữu đang giữ nó (atomic "check rồi xóa"),
    // tránh trường hợp xóa nhầm lock của người khác sau khi lock cũ đã hết hạn.
    private static final String RELEASE_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('DEL', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String lockKey(String flightId, String seatId) {
        return "lock:flight:" + flightId + ":seat:" + seatId;
    }

    /**
     * SET NX EX - atomic trong Redis, nên khi 2 request đến cùng lúc,
     * chỉ 1 request nhận được true (thắng), request còn lại nhận false ngay lập tức.
     */
    public boolean acquireSeatLock(String flightId, String seatId, String ownerId, long ttlSeconds) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(lockKey(flightId, seatId), ownerId, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(result);
    }

    public boolean releaseSeatLock(String flightId, String seatId, String ownerId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(lockKey(flightId, seatId)), ownerId);
        return result != null && result == 1L;
    }

    public String getLockOwner(String flightId, String seatId) {
        return redisTemplate.opsForValue().get(lockKey(flightId, seatId));
    }
}
