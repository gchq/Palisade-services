/*
 * Copyright 2018-2021 Crown Copyright
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

package uk.gov.gchq.palisade.service.attributemask.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisKeyValueAdapter.EnableKeyspaceEvents;
import org.springframework.data.redis.core.convert.KeyspaceConfiguration;
import org.springframework.data.redis.core.convert.MappingConfiguration;
import org.springframework.data.redis.core.index.IndexConfiguration;
import org.springframework.data.redis.core.mapping.RedisMappingContext;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.lang.NonNull;

import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Additional Redis configuration to set time-to-live on a per-keyspace basis.
 * This must still be enabled in the domain entity with the {@link org.springframework.data.redis.core.TimeToLive} annotation.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "spring.data.redis.repositories",
        name = "enabled",
        havingValue = "true"
)
@EnableRedisRepositories(enableKeyspaceEvents = EnableKeyspaceEvents.ON_STARTUP, basePackageClasses = {AttributeMaskingApplication.class})
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfiguration {
    protected static final Map<String, Long> KEYSPACE_TTL = new ConcurrentHashMap<>();
    protected static final List<Class<?>> REDIS_KEYSPACE = List.of(AuthorisedRequestEntity.class);

    /**
     * Configure a key prefix for redis entities
     *
     * @param prefix the value to be used for the entity prefix
     * @return a {@code KeyspaceConfiguration} object
     */
    KeyspaceConfiguration getKeyspaceConfiguration(final String prefix) {
        // There's some aspect on the initialConfiguration that makes it execute before the constructor
        // Create a class instance and return like a factory instead (closure on 'prefix')
        return new KeyspaceConfiguration() {
            @Override
            @NonNull
            public Iterable<KeyspaceSettings> initialConfiguration() {
                return REDIS_KEYSPACE.stream()
                        .map(type -> new KeyspaceSettings(type, prefix + type.getSimpleName()))
                        .collect(Collectors.toList());
            }
        };
    }

    /**
     * Get the time-to-live in seconds for a given keyspace name.
     *
     * @param keyspace the name of the redis keyspace
     * @return the configured time-to-live value for that keyspace in seconds
     */
    public static Long getTimeToLiveSeconds(final String keyspace) {
        return KEYSPACE_TTL.getOrDefault(keyspace, RedisProperties.getDefaultTtl().toSeconds());
    }


    @Bean
    RedisMappingContext keyValueMappingContext(final RedisProperties properties) {
        IndexConfiguration indexConfiguration = new IndexConfiguration();
        KeyspaceConfiguration keyspaceConfiguration = getKeyspaceConfiguration(properties.getKeyPrefix());
        return new RedisMappingContext(new MappingConfiguration(indexConfiguration, keyspaceConfiguration));
    }

    @Bean
    Map<String, Long> redisTimeToLive(final RedisProperties additionalProperties) {
        additionalProperties.getTimeToLive().forEach((key, value) -> KEYSPACE_TTL.put(key, value.toSeconds()));
        return KEYSPACE_TTL;
    }

}
