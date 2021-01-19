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

package uk.gov.gchq.palisade.service.resource.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import uk.gov.gchq.palisade.service.resource.domain.CompletenessEntity;
import uk.gov.gchq.palisade.service.resource.domain.ResourceEntity;
import uk.gov.gchq.palisade.service.resource.domain.SerialisedFormatEntity;
import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;
import uk.gov.gchq.palisade.service.resource.repository.ReactiveRepositoryRedisAdapter.CompletenessRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.ReactiveRepositoryRedisAdapter.ResourceRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.ReactiveRepositoryRedisAdapter.SerialisedFormatRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.ReactiveRepositoryRedisAdapter.TypeRepositoryAdapter;

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
public class RedisConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfiguration.class);
    private final ReactiveRedisConnectionFactory factory;

    /**
     * Public constructor for {@link RedisConfiguration}
     *
     * @param factory the reactive connection factory for redis
     */
    public RedisConfiguration(final ReactiveRedisConnectionFactory factory) {
        this.factory = factory;
        LOGGER.debug("Initialised Redis repositories");
    }

    /**
     * Creates the {@link ReactiveRedisTemplate}
     *
     * @param hkClass the class for the key
     * @param hvClass the class for the value
     * @param <K> the key
     * @param <V> the value
     * @return a {@link ReactiveRedisTemplate}
     */
    public <K, V> ReactiveRedisTemplate<String, V> reactiveRedisTemplate(final Class<K> hkClass, final Class<V> hvClass) {
        StringRedisSerializer tSerde = new StringRedisSerializer();
        RedisSerializer<K> kSerde = new Jackson2JsonRedisSerializer<>(hkClass);
        RedisSerializer<V> vSerde = new Jackson2JsonRedisSerializer<>(hvClass);
        RedisSerializationContext<String, V> context =
                RedisSerializationContext.<String, V>newSerializationContext()
                        .key(tSerde)
                        .value(vSerde)
                        .hashKey(kSerde)
                        .hashValue(vSerde)
                        .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Primary
    @Bean
    CompletenessRepositoryAdapter completenessRepositoryAdapter() {
        return new CompletenessRepositoryAdapter(reactiveRedisTemplate(Integer.class, CompletenessEntity.class));
    }

    @Primary
    @Bean
    ResourceRepositoryAdapter resourceRepositoryAdapter() {
        return new ResourceRepositoryAdapter(reactiveRedisTemplate(String.class, ResourceEntity.class));
    }

    @Primary
    @Bean
    TypeRepositoryAdapter typeRepositoryAdapter() {
        return new TypeRepositoryAdapter(reactiveRedisTemplate(String.class, TypeEntity.class));
    }

    @Primary
    @Bean
    SerialisedFormatRepositoryAdapter serialisedFormatRepositoryAdapter() {
        return new SerialisedFormatRepositoryAdapter(reactiveRedisTemplate(String.class, SerialisedFormatEntity.class));
    }
}
