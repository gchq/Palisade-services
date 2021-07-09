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

package uk.gov.gchq.palisade.service.resource.repository;

import org.reactivestreams.Publisher;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import uk.gov.gchq.palisade.service.resource.domain.CompletenessEntity;
import uk.gov.gchq.palisade.service.resource.domain.EntityType;
import uk.gov.gchq.palisade.service.resource.domain.ResourceEntity;
import uk.gov.gchq.palisade.service.resource.domain.SerialisedFormatEntity;
import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;
import uk.gov.gchq.palisade.service.resource.exception.ReactiveRepositoryReflectionException;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A class that implements a {@link ReactiveCrudRepository} for Redis backing store
 *
 * @param <V> the value in the backing store
 * @param <K> the key in the backing store
 */
public abstract class AbstractReactiveRepositoryRedisAdapter<V, K> implements ReactiveCrudRepository<V, K> {
    public static final String KEY_SEP = "::";
    private static final String ID_KEYSPACE = "id";
    protected final String table;
    protected final ReactiveHashOperations<String, K, V> hashOps;
    protected final ReactiveSetOperations<String, K> setOps;
    protected final ReactiveValueOperations<String, V> valueOps;
    protected final ReactiveRedisTemplate<String, V> redisTemplate;

    protected AbstractReactiveRepositoryRedisAdapter(final ReactiveRedisTemplate<String, V> redisTemplate, final String table) {
        this.redisTemplate = redisTemplate;
        this.table = table;
        RedisSerializationContext<String, V> ctx = redisTemplate.getSerializationContext();
        this.hashOps = redisTemplate.opsForHash();
        this.setOps = redisTemplate.opsForSet(RedisSerializationContext.<String, K>newSerializationContext()
                .key(ctx.getKeySerializationPair())
                .value(ctx.getHashKeySerializationPair())
                .hashKey(ctx.getHashKeySerializationPair())
                .hashValue(ctx.getHashValueSerializationPair())
                .build());
        this.valueOps = redisTemplate.opsForValue(ctx);
    }

    protected static <S, K> K reflectIdAnnotation(final S entity) {
        return Arrays.stream(entity.getClass().getDeclaredFields())
                .filter(field -> field.getAnnotation(Id.class) != null)
                .map((Field field) -> {
                    try {
                        field.trySetAccessible();
                        return (K) field.get(entity);
                    } catch (IllegalAccessException e) {
                        throw new ReactiveRepositoryReflectionException(String.format("Failed to reflect field %s for %s", field, entity), e);
                    }
                })
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(String.format("Failed to find field annotated with %s for %s", Id.class, entity.getClass())));
    }

    protected static <S> Table reflectTableAnnotation(final Class<S> entityClass) {
        return entityClass.getAnnotation(Table.class);
    }

    @Override
    @NonNull
    public abstract <S extends V> Mono<S> save(final @NonNull S entity);

    protected <S extends V> Mono<S> saveDefault(final @NonNull S entity) {
        K id = reflectIdAnnotation(entity);
        // The ttl set here should be moved to configuration
        return this.valueOps.set(this.table + KEY_SEP + ID_KEYSPACE + KEY_SEP + id, entity)
                .then(this.redisTemplate.expire(this.table + KEY_SEP + ID_KEYSPACE + KEY_SEP + id, Duration.ofMinutes(5L)))
                .filter(bool -> bool)
                .map(bool -> entity);
    }

    @Override
    @NonNull
    public final <S extends V> Flux<S> saveAll(final @NonNull Iterable<S> entities) {
        return Flux.fromIterable(entities)
                .flatMap(this::save);
    }

    @Override
    @NonNull
    public final <S extends V> Flux<S> saveAll(final @NonNull Publisher<S> entityStream) {
        return Flux.from(entityStream)
                .flatMap(this::save);
    }

    @Override
    @NonNull
    public Mono<V> findById(final @NonNull K key) {
        return this.valueOps.get(this.table + KEY_SEP + ID_KEYSPACE + KEY_SEP + key);
    }

    @Override
    @NonNull
    public final Mono<V> findById(final @NonNull Publisher<K> id) {
        return Mono.from(id)
                .flatMap(this::findById);
    }

    @Override
    @NonNull
    public Mono<Boolean> existsById(final @NonNull K key) {
        return this.valueOps.get(this.table + KEY_SEP + ID_KEYSPACE + KEY_SEP + key)
                .hasElement();
    }

    @Override
    @NonNull
    public final Mono<Boolean> existsById(final @NonNull Publisher<K> id) {
        return Mono.from(id)
                .flatMap(this::existsById);
    }

    @Override
    @NonNull
    public Flux<V> findAll() {
        return this.redisTemplate.keys(this.table + KEY_SEP + ID_KEYSPACE + KEY_SEP + "*")
                .flatMap(key -> this.findById((K) key));
    }

    @Override
    @NonNull
    public Flux<V> findAllById(final @NonNull Iterable<K> ids) {
        LinkedList<K> idsList = new LinkedList<>();
        ids.forEach(idsList::add);
        return this.valueOps.multiGet(idsList.stream()
                .map(id -> this.table + KEY_SEP + ID_KEYSPACE + KEY_SEP + id).collect(Collectors.toList()))
                .flux()
                .flatMapIterable(Function.identity());
    }

    @Override
    @NonNull
    public final Flux<V> findAllById(final @NonNull Publisher<K> idStream) {
        return Flux.from(idStream)
                .flatMap(this::findById);
    }

    @Override
    @NonNull
    public Mono<Long> count() {
        return this.findAll().count();
    }

    @Override
    @NonNull
    public abstract Mono<Void> deleteById(final @NonNull K key);

    /**
     * Deletes a record using the key value
     *
     * @param key the key of the record to be deleted
     * @return a {@link Mono} of type {@link Void}
     */
    public Mono<Void> deleteByIdDefault(final @NonNull K key) {
        return this.valueOps.delete(this.table + KEY_SEP + ID_KEYSPACE + KEY_SEP + key)
                .then();
    }

    @Override
    @NonNull
    public final Mono<Void> deleteById(final @NonNull Publisher<K> id) {
        return Mono.from(id)
                .flatMap(this::deleteById);
    }

    @Override
    @NonNull
    public final Mono<Void> delete(final @NonNull V entity) {
        return this.deleteById(AbstractReactiveRepositoryRedisAdapter.<V, K>reflectIdAnnotation(entity));
    }

    @Override
    @NonNull
    public Mono<Void> deleteAll(final @NonNull Iterable<? extends V> entities) {
        return this.deleteAll(Flux.fromIterable(entities));
    }

    @Override
    @NonNull
    public final Mono<Void> deleteAll(final @NonNull Publisher<? extends V> entityStream) {
        return Flux.from(entityStream)
                .flatMap(this::delete)
                .then();
    }

    @Override
    @NonNull
    public Mono<Void> deleteAll() {
        return this.findAll()
                .flatMap(this::delete)
                .then();
    }

    /**
     * A class to allow {@link CompletenessEntity}s to be stored in a reactive repository
     */
    public static class CompletenessRepositoryAdapter extends AbstractReactiveRepositoryRedisAdapter<CompletenessEntity, Integer> implements CompletenessRepository {

        /**
         * {@link CompletenessRepositoryAdapter} constructor that takes a redis template
         *
         * @param redisTemplate a {@link ReactiveRedisTemplate} with key of type {@link String} and value of type {@link CompletenessEntity}
         * @param keyPrefix     a prefix to the table 'namespace'
         */
        public CompletenessRepositoryAdapter(final ReactiveRedisTemplate<String, CompletenessEntity> redisTemplate, final String keyPrefix) {
            super(redisTemplate, keyPrefix + reflectTableAnnotation(CompletenessEntity.class).value());
        }

        @Override
        public Mono<CompletenessEntity> findOneByEntityTypeAndEntityId(final EntityType entityType, final String entityId) {
            return this.valueOps.get(this.table + KEY_SEP + CompletenessEntity.idFor(entityType, entityId).toString());
        }

        @Override
        @NonNull
        public <S extends CompletenessEntity> Mono<S> save(final @NonNull S entity) {
            final String id = this.table + KEY_SEP + entity.getId();
            // The ttl set here should be moved to configuration
            return this.valueOps.set(id, entity)
                    .then(this.redisTemplate.expire(id, Duration.ofMinutes(3L)))
                    .thenReturn(entity);
        }

        @Override
        @NonNull
        public Mono<Void> deleteById(final @NonNull Integer key) {
            return this.valueOps.delete(this.table + KEY_SEP + key.toString())
                    .then();
        }
    }

    /**
     * A class to allow {@link ResourceEntity}s to be stored in a reactive repository
     */
    public static class ResourceRepositoryAdapter extends AbstractReactiveRepositoryRedisAdapter<ResourceEntity, String> implements ResourceRepository {

        private static final String PARENT_KEYSPACE = "parent";

        /**
         * {@link ResourceRepositoryAdapter} constructor that takes a redis template
         *
         * @param redisTemplate a {@link ReactiveRedisTemplate} with key of type {@link String} and value of type {@link ResourceEntity}
         * @param keyPrefix     a prefix to the table 'namespace'
         */
        public ResourceRepositoryAdapter(final ReactiveRedisTemplate<String, ResourceEntity> redisTemplate, final String keyPrefix) {
            super(redisTemplate, keyPrefix + reflectTableAnnotation(ResourceEntity.class).value());
        }

        @Override
        public Mono<ResourceEntity> findOneByResourceId(final String resourceId) {
            return this.findById(resourceId);
        }

        @Override
        @NonNull
        public <S extends ResourceEntity> Mono<S> save(final @NonNull S entity) {
            final String id = this.table + KEY_SEP + PARENT_KEYSPACE + KEY_SEP + entity.getParentId();
            // The ttl set here should be moved to configuration
            return this.setOps.add(id, entity.getId())
                    .then(this.redisTemplate.expire(id, Duration.ofMinutes(5L)))
                    .then(this.saveDefault(entity));
        }

        @Override
        public Flux<ResourceEntity> findAllByParentId(final String parentId) {
            return this.setOps.members(this.table + KEY_SEP + PARENT_KEYSPACE + KEY_SEP + parentId)
                    .flatMap(this::findById);
        }

        @Override
        @NonNull
        public Mono<Void> deleteById(final @NonNull String key) {
            return this.findById(key)
                    .map(entity -> this.setOps.remove(this.table + KEY_SEP + PARENT_KEYSPACE + KEY_SEP + entity.getParentId()))
                    .then(this.deleteByIdDefault(key));
        }
    }

    /**
     * A class to allow {@link SerialisedFormatEntity}s to be stored in a reactive repository
     */
    public static class SerialisedFormatRepositoryAdapter extends AbstractReactiveRepositoryRedisAdapter<SerialisedFormatEntity, String> implements SerialisedFormatRepository {

        private static final String FORMAT_KEYSPACE = "format";

        /**
         * {@link SerialisedFormatRepositoryAdapter} constructor that takes a redis template
         *
         * @param redisTemplate a {@link ReactiveRedisTemplate} with key of type {@link String} and value of type {@link SerialisedFormatEntity}
         * @param keyPrefix     a prefix to the table 'namespace'
         */
        public SerialisedFormatRepositoryAdapter(final ReactiveRedisTemplate<String, SerialisedFormatEntity> redisTemplate, final String keyPrefix) {
            super(redisTemplate, keyPrefix + reflectTableAnnotation(SerialisedFormatEntity.class).value());
        }

        @Override
        public Mono<SerialisedFormatEntity> findOneByResourceId(final String resourceId) {
            return this.findById(resourceId);
        }

        @Override
        @NonNull
        public <S extends SerialisedFormatEntity> Mono<S> save(final @NonNull S entity) {
            final String id = this.table + KEY_SEP + FORMAT_KEYSPACE + KEY_SEP + entity.getSerialisedFormat();
            // The ttl set here should be moved to configuration
            return this.setOps.add(id, entity.getId())
                    .then(this.redisTemplate.expire(id, Duration.ofMinutes(5L)))
                    .then(this.saveDefault(entity));
        }

        @Override
        public Flux<SerialisedFormatEntity> findAllBySerialisedFormat(final String serialisedFormat) {
            return this.setOps.members(this.table + KEY_SEP + FORMAT_KEYSPACE + KEY_SEP + serialisedFormat)
                    .flatMap(this::findById);
        }

        @Override
        @NonNull
        public Mono<Void> deleteById(final @NonNull String key) {
            return this.findById(key)
                    .map(entity -> this.setOps.remove(this.table + KEY_SEP + FORMAT_KEYSPACE + KEY_SEP + entity.getSerialisedFormat()))
                    .then(this.deleteByIdDefault(key));
        }
    }

    /**
     * A class to allow {@link TypeRepository}s to be stored in a reactive repository
     */
    public static class TypeRepositoryAdapter extends AbstractReactiveRepositoryRedisAdapter<TypeEntity, String> implements TypeRepository {

        private static final String TYPE_KEYSPACE = "type";

        /**
         * {@link TypeRepositoryAdapter} constructor that takes a redis template
         *
         * @param redisTemplate a {@link ReactiveRedisTemplate} with key of type {@link String} and value of type {@link TypeEntity}
         * @param keyPrefix     a prefix to the table 'namespace'
         */
        public TypeRepositoryAdapter(final ReactiveRedisTemplate<String, TypeEntity> redisTemplate, final String keyPrefix) {
            super(redisTemplate, keyPrefix + reflectTableAnnotation(TypeEntity.class).value());
        }

        @Override
        public Mono<TypeEntity> findOneByResourceId(final String resourceId) {
            return this.findById(resourceId);
        }

        @Override
        @NonNull
        public <S extends TypeEntity> Mono<S> save(final @NonNull S entity) {
            final String id = this.table + KEY_SEP + TYPE_KEYSPACE + KEY_SEP + entity.getType();
            // The ttl set here should be moved to configuration
            return this.setOps.add(id, entity.getId())
                    .then(this.redisTemplate.expire(id, Duration.ofMinutes(5L)))
                    .then(super.saveDefault(entity));
        }

        @Override
        public Flux<TypeEntity> findAllByType(final String type) {
            return this.setOps.members(this.table + KEY_SEP + TYPE_KEYSPACE + KEY_SEP + type)
                    .flatMap(this::findById);
        }

        @Override
        @NonNull
        public Mono<Void> deleteById(final @NonNull String key) {
            return this.findById(key)
                    .map(entity -> this.setOps.remove(this.table + KEY_SEP + TYPE_KEYSPACE + KEY_SEP + entity.getType()))
                    .then(super.deleteByIdDefault(key));
        }
    }
}
