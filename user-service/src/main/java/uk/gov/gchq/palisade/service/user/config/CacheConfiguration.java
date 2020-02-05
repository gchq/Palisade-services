/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.palisade.service.user.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheConfiguration {
    private Integer maximumSize;
    private Duration expireAfterWrite;
    private List<String> cacheNames;

    public Integer getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(final Integer maximumSize) {
        this.maximumSize = maximumSize;
    }

    public Duration getExpireAfterWrite() {
        return expireAfterWrite;
    }

    public void setExpireAfterWrite(final Duration expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
    }

    public List<String> getCacheNames() {
        return cacheNames;
    }

    public void setCacheNames(final List<String> cacheNames) {
        this.cacheNames = cacheNames;
    }

    @Bean
    @ConditionalOnProperty(name = "type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(cacheNames);
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWrite)
                .recordStats();
    }

    @Bean
    @ConditionalOnProperty(name = "type", havingValue = "redis")
    public CacheManager redisCacheManager() throws NotImplementedException {
        throw new NotImplementedException("Redis not implemented yet");
    }

}