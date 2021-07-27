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

package uk.gov.gchq.palisade.service.resource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.Generated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration for redis properties.
 */
@ConfigurationProperties("spring.data.redis.repositories")
public class RedisProperties {
    private static final Duration DEFAULT_TTL = Duration.ofDays(1);
    private Map<String, Duration> timeToLive = new HashMap<>();
    private String keyPrefix = "";

    @Generated
    public Map<String, Duration> getTimeToLive() {
        return timeToLive;
    }

    @Generated
    public static Duration getDefaultTtl() {
        return DEFAULT_TTL;
    }

    @Generated
    public void setTimeToLive(final Map<String, Duration> timeToLive) {
        this.timeToLive = timeToLive;
    }

    @Generated
    public String getKeyPrefix() {
        return keyPrefix;
    }

    @Generated
    public void setKeyPrefix(final String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * Get the time to live for a given table name, or default if not found.
     * This should be preferred over {@link RedisProperties#getTimeToLive()}.
     *
     * @param tableName the name of a table in the configuration to get a time-to-live value for
     * @return that table's time-to-live duration, or the default (1 day) if no such configuration key was found
     */
    @Generated
    public Duration getTimeToLive(final String tableName) {
        return this.timeToLive.getOrDefault(tableName, DEFAULT_TTL);
    }
}
