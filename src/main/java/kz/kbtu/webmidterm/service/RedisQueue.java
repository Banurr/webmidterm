package kz.kbtu.webmidterm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RedisQueue {

    private static final String WAITING_KEY = "queue:waiting";
    private static final String COUNTER_KEY = "queue:counter:";

    private final StringRedisTemplate redis;

    public void add(Long ticketId) {
        redis.opsForZSet().add(WAITING_KEY, ticketId.toString(), ticketId.doubleValue());
    }

    public Long position(Long ticketId) {
        return redis.opsForZSet().rank(WAITING_KEY, ticketId.toString());
    }

    public boolean remove(Long ticketId) {
        Long removed = redis.opsForZSet().remove(WAITING_KEY, ticketId.toString());
        return removed != null && removed > 0;
    }

    public Long popNext() {
        ZSetOperations.TypedTuple<String> next = redis.opsForZSet().popMin(WAITING_KEY);
        if (next == null || next.getValue() == null) {
            return null;
        }
        return Long.valueOf(next.getValue());
    }

    public long size() {
        Long size = redis.opsForZSet().size(WAITING_KEY);
        return size == null ? 0 : size;
    }

    public String nextNumber(String serviceCode) {
        String key = COUNTER_KEY + serviceCode + ":" + LocalDate.now();
        Long counter = redis.opsForValue().increment(key);
        redis.expire(key, Duration.ofDays(2));
        return String.format("%s-%03d", serviceCode, counter);
    }
}
