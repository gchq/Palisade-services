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
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Additional Redis configuration to set time-to-live on a per-keyspace basis.
 * This must still be enabled in the domain entity with the {@link TimeToLive} annotation.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "spring.data.redis.repositories",
        name = "enabled",
        havingValue = "true"
)
@EnableRedisRepositories(enableKeyspaceEvents = EnableKeyspaceEvents.ON_STARTUP, basePackageClasses = {AttributeMaskingApplication.class})
@EnableConfigurationProperties(RedisTtlProperties.class)
public class RedisTtlConfiguration {
    protected static final Map<String, Long> KEYSPACE_TTL = new ConcurrentHashMap<>();

    /**
     * Get the time-to-live in seconds for a given keyspace name
     *
     * @param keyspace the name of the redis keyspace
     * @return the configured time-to-live value for that keyspace in seconds
     */
    public static Long getTimeToLiveSeconds(final String keyspace) {
        return KEYSPACE_TTL.getOrDefault(keyspace, RedisTtlProperties.getDefaultTtl().toSeconds());
    }

    @Bean
    RedisTtlProperties additionalProperties() {
        return new RedisTtlProperties();
    }

    @Bean
    Map<String, Long> redisTimeToLive(final RedisTtlProperties additionalProperties) {
        additionalProperties.getTimeToLive().forEach((key, value) -> KEYSPACE_TTL.put(key, value.toSeconds()));
        return KEYSPACE_TTL;
    }

}
