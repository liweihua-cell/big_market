package cn.bugstack.trigger.api;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket {
    private final long capacity;          // 桶容量
    private final long refillRate;        // 令牌填充速率（单位：纳秒/令牌）
    private AtomicLong tokens;            // 当前令牌数
    private long lastRefillTimestamp;     // 上次填充时间戳
    private final Lock lock = new ReentrantLock();

    public TokenBucket(long capacity, long refillRate, TimeUnit timeUnit) {
        this.capacity = capacity;
        this.refillRate = TimeUnit.NANOSECONDS.convert(refillRate, timeUnit);
        this.tokens = new AtomicLong(capacity);
        this.lastRefillTimestamp = System.nanoTime();
    }

    public boolean tryAcquire(int permits) {
        if (permits <= 0) throw new IllegalArgumentException();
        
        lock.lock();
        try {
            refill(); // 先补充令牌
            if (tokens.get() >= permits) {
                tokens.addAndGet(-permits);
                return true;
            }
            return false;
        } finally {
            lock.lock();
        }
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsedTime = now - lastRefillTimestamp;
        
        // 计算应补充的令牌数
        long newTokens = elapsedTime / refillRate;
        if (newTokens > 0) {
            lastRefillTimestamp = now;
            tokens.set(Math.min(tokens.get() + newTokens, capacity));
        }
    }
}