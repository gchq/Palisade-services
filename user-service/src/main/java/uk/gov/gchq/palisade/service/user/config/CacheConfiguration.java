package uk.gov.gchq.palisade.service.user.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@EnableCaching
@Configuration
public class CacheConfiguration extends CachingConfigurerSupport {

    @Value("${cache.caffeine.spec.expireAfterAccess}")
    public Duration duration;

    @Value("${cache.caffeine.spec.maximumSize}")
    public long maximumSize;

    @Override
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager() {

            @Override
            protected Cache createConcurrentMapCache(final String cacheNames) {
                return new ConcurrentMapCache(cacheNames,
                        Caffeine.newBuilder().expireAfterWrite(duration).maximumSize(maximumSize).build().asMap(), false);
            }
        };
        return cacheManager;
    }
}
