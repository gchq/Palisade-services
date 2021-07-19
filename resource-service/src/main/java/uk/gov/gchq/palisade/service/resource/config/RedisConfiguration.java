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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import uk.gov.gchq.palisade.service.resource.domain.ResourceConverter;
import uk.gov.gchq.palisade.service.resource.domain.ResourceEntity;
import uk.gov.gchq.palisade.service.resource.domain.SerialisedFormatEntity;
import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter.CompletenessRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter.ResourceRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter.SerialisedFormatRepositoryAdapter;
import uk.gov.gchq.palisade.service.resource.repository.AbstractReactiveRepositoryRedisAdapter.TypeRepositoryAdapter;

/**
 * Additional Redis configuration to set key prefixes and serialisation context.
 */
@Configuration
@ConditionalOnProperty(
        prefix = "spring.data.redis.repositories",
        name = "enabled",
        havingValue = "true"
)
@EnableConfigurationProperties(RedisProperties.class)
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
     * @param kSerde the serializer class for the key
     * @param vSerde the serializer class for the value
     * @param <K>    the key
     * @param <V>    the value
     * @return a {@link ReactiveRedisTemplate}
     */
    public <K, V> ReactiveRedisTemplate<String, V> reactiveRedisTemplate(final RedisSerializer<K> kSerde, final RedisSerializer<V> vSerde) {
        StringRedisSerializer tSerde = new StringRedisSerializer();
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
    CompletenessRepositoryAdapter completenessRepositoryAdapter(final RedisProperties properties) {
        RedisSerializer<Integer> kSerde = new Jackson2JsonRedisSerializer<>(Integer.class);
        RedisSerializer<CompletenessEntity> vSerde = new Jackson2JsonRedisSerializer<>(CompletenessEntity.class);
        return new CompletenessRepositoryAdapter(reactiveRedisTemplate(kSerde, vSerde), properties);
    }

    @Primary
    @Bean
    ResourceRepositoryAdapter resourceRepositoryAdapter(final RedisProperties properties) {
        RedisSerializer<String> kSerde = new Jackson2JsonRedisSerializer<>(String.class);
        Jackson2JsonRedisSerializer<ResourceEntity> vSerde = new Jackson2JsonRedisSerializer<>(ResourceEntity.class);
        vSerde.setObjectMapper(ResourceConverter.MAPPER);
        return new ResourceRepositoryAdapter(reactiveRedisTemplate(kSerde, vSerde), properties);
    }

    @Primary
    @Bean
    TypeRepositoryAdapter typeRepositoryAdapter(final RedisProperties properties) {
        RedisSerializer<String> kSerde = new Jackson2JsonRedisSerializer<>(String.class);
        RedisSerializer<TypeEntity> vSerde = new Jackson2JsonRedisSerializer<>(TypeEntity.class);
        return new TypeRepositoryAdapter(reactiveRedisTemplate(kSerde, vSerde), properties);
    }

    @Primary
    @Bean
    SerialisedFormatRepositoryAdapter serialisedFormatRepositoryAdapter(final RedisProperties properties) {
        RedisSerializer<String> kSerde = new Jackson2JsonRedisSerializer<>(String.class);
        RedisSerializer<SerialisedFormatEntity> vSerde = new Jackson2JsonRedisSerializer<>(SerialisedFormatEntity.class);
        return new SerialisedFormatRepositoryAdapter(reactiveRedisTemplate(kSerde, vSerde), properties);
    }
}
