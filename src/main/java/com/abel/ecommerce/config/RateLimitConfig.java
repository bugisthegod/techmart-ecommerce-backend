package com.abel.ecommerce.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    // Global rate limit: 100 requests per second per IP
    public static final long IP_RATE_LIMIT = 100;

    // User rate limit: 50 requests per second per user
    public static final long USER_RATE_LIMIT = 50;

    // Seckill rate limit: 10 requests per second per user
    public static final long SECKILL_RATE_LIMIT = 10;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedisClient lettuceRedisClient() {
        String protocol = "redis";
        String userInfo = "";

        if (redisPassword != null && !redisPassword.isBlank()) {
            protocol = "rediss"; //
            userInfo = ":" + redisPassword + "@";
        }

        String redisUri = String.format("%s://%s%s:%d", protocol, userInfo, redisHost, redisPort);
        return RedisClient.create(redisUri);
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(RedisClient lettuceRedisClient) {
        return lettuceRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public ProxyManager<String> rateLimitProxyManager(StatefulRedisConnection<String, byte[]> rateLimitRedisConnection) {
        return LettuceBasedProxyManager.builderFor(rateLimitRedisConnection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(5)))
                .build();
    }

    // IP rate limit configuration
    @Bean
    public BucketConfiguration ipRateLimitConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(IP_RATE_LIMIT)
                        .refillGreedy(IP_RATE_LIMIT, Duration.ofSeconds(1))
                        .build())
                .build();
    }

    // User rate limit configuration
    @Bean
    public BucketConfiguration userRateLimitConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(USER_RATE_LIMIT)
                        .refillGreedy(USER_RATE_LIMIT, Duration.ofSeconds(1))
                        .build())
                .build();
    }

    // Seckill rate limit configuration
    @Bean
    public BucketConfiguration seckillRateLimitConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(SECKILL_RATE_LIMIT)
                        .refillGreedy(SECKILL_RATE_LIMIT, Duration.ofSeconds(1))
                        .build())
                .build();
    }
}
