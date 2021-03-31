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

import org.springframework.boot.context.properties.ConfigurationProperties;

import uk.gov.gchq.palisade.service.attributemask.common.Generated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration for redis keyspace ttl properties - duration in seconds per keyspace name.
 */
@ConfigurationProperties("spring.data.redis.repositories")
public class RedisTtlProperties {
    private static final Duration DEFAULT_TTL = Duration.ofDays(1);
    private Map<String, Duration> timeToLive = new HashMap<>();

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
}
