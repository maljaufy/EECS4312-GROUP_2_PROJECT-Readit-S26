package com.redditclone.shared.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String POST_FEED_CACHE = "postFeed";

    @Bean
    public RedisCacheManagerBuilderCustomizer postFeedCacheCustomizer() {
        return builder -> builder.withCacheConfiguration(
                POST_FEED_CACHE,
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(60))
                        .disableCachingNullValues());
    }
}
