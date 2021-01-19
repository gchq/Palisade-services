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

package uk.gov.gchq.palisade.service.resource.repository;

import org.reactivestreams.Publisher;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Function;

/**
 * A class that implements a {@link ReactiveCrudRepository} for Redis backing store
 *
 * @param <V> the value in the backing store
 * @param <K> the key in the backing store
 */
public abstract class ReactiveRepositoryRedisAdapter<V, K> implements ReactiveCrudRepository<V, K> {
    public static final String KEY_SEP = "::";
    protected final String table;
    protected final ReactiveHashOperations<String, K, V> hashOps;
    protected final ReactiveSetOperations<String, K> setOps;

    protected ReactiveRepositoryRedisAdapter(final ReactiveRedisTemplate<String, V> redisTemplate, final String table) {
        this.table = table;
        RedisSerializationContext<String, V> ctx = redisTemplate.getSerializationContext();
        this.hashOps = redisTemplate.opsForHash();
        this.setOps = redisTemplate.opsForSet(RedisSerializationContext.<String, K>newSerializationContext()
                .key(ctx.getKeySerializationPair())
                .value(ctx.getHashKeySerializationPair())
                .hashKey(ctx.getHashKeySerializationPair())
                .hashValue(ctx.getHashValueSerializationPair())
                .build());
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
        return this.hashOps.put(this.table, id, entity)
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
        return this.hashOps.get(this.table, key);
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
        return this.hashOps.hasKey(this.table, key);
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
        return this.hashOps.values(this.table);
    }

    @Override
    @NonNull
    public Flux<V> findAllById(final @NonNull Iterable<K> ids) {
        LinkedList<K> idsList = new LinkedList<>();
        ids.forEach(idsList::add);
        return this.hashOps.multiGet(this.table, idsList)
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
        return this.hashOps.size(this.table);
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
        return this.hashOps.remove(this.table, key)
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
        return this.deleteById(ReactiveRepositoryRedisAdapter.<V, K>reflectIdAnnotation(entity));
    }

    @Override
    @NonNull
    public Mono<Void> deleteAll(final @NonNull Iterable<? extends V> entities) {
        return this.hashOps.remove(this.table, entities)
                .then();
    }

    @Override
    @NonNull
    public final Mono<Void> deleteAll(final @NonNull Publisher<? extends V> entityStream) {
        return Flux.from(entityStream)
                .map(this::delete)
                .then();
    }

    @Override
    @NonNull
    public Mono<Void> deleteAll() {
        return this.hashOps.delete(this.table)
                .then();
    }

    /**
     * A class to allow {@link CompletenessEntity}s to be stored in a reactive repository
     */
    public static class CompletenessRepositoryAdapter extends ReactiveRepositoryRedisAdapter<CompletenessEntity, Integer> implements CompletenessRepository {

        /**
         * {@link CompletenessRepositoryAdapter} constructor that takes a redis template
         *
         * @param redisTemplate a {@link ReactiveRedisTemplate} with key of type {@link String} and value of type {@link CompletenessEntity}
         */
        public CompletenessRepositoryAdapter(final ReactiveRedisTemplate<String, CompletenessEntity> redisTemplate) {
            super(redisTemplate, reflectTableAnnotation(CompletenessEntity.class).value());
        }

        @Override
        public Mono<CompletenessEntity> findOneByEntityTypeAndEntityId(final EntityType entityType, final String entityId) {
            return this.findById(CompletenessEntity.idFor(entityType, entityId));
        }

        @Override
        @NonNull
        public <S extends CompletenessEntity> Mono<S> save(final @NonNull S entity) {
            return this.saveDefault(entity);
        }

        @Override
        @NonNull
        public Mono<Void> deleteById(final @NonNull Integer key) {
            return this.deleteByIdDefault(key);
        }
    }

    /**
     * A class to allow {@link ResourceEntity}s to be stored in a reactive repository
     */
    public static class ResourceRepositoryAdapter extends ReactiveRepositoryRedisAdapter<ResourceEntity, String> implements ResourceRepository {

        /**
         * {@link ResourceRepositoryAdapter} constructor that takes a redis template
         *
         * @param redisTemplate a {@link ReactiveRedisTemplate} with key of type {@link String} and value of type {@link ResourceEntity}
         */
        public ResourceRepositoryAdapter(final ReactiveRedisTemplate<String, ResourceEntity> redisTemplate) {
            super(redisTemplate, reflectTableAnnotation(ResourceEntity.class).value());
        }

        @Override
        public Mono<ResourceEntity> findOneByResourceId(final String resourceId) {
            return this.findById(resourceId);
        }

        @Override
        @NonNull
        public <S extends ResourceEntity> Mono<S> save(final @NonNull S entity) {
            return this.setOps.add(this.table + KEY_SEP + entity.getParentId(), entity.getId())
                    .then(super.saveDefault(entity));
        }

        @Override
        public Flux<ResourceEntity> findAllByParentId(final String parentId) {
            return this.setOps.members(this.table + KEY_SEP + parentId)
                    .flatMap(this::findById);
        }

        @Override
        @NonNull
        public Mono<Void> deleteById(final @NonNull String key) {
            return this.findById(key)
                    .map(entity -> this.setOps.remove(this.table + KEY_SEP + entity.getParentId()))
                    .then(super.deleteByIdDefault(key));
        }
    }

    /**
     * A class to allow {@link SerialisedFormatEntity}s to be stored in a reactive repository
     */
    public static class SerialisedFormatRepositoryAdapter extends ReactiveRepositoryRedisAdapter<SerialisedFormatEntity, String> implements SerialisedFormatRepository {

        /**
         * {@link SerialisedFormatRepositoryAdapter} constructor that takes a redis template
         *
         * @param redisTemplate a {@link ReactiveRedisTemplate} with key of type {@link String} and value of type {@link SerialisedFormatEntity}
         */
        public SerialisedFormatRepositoryAdapter(final ReactiveRedisTemplate<String, SerialisedFormatEntity> redisTemplate) {
            super(redisTemplate, reflectTableAnnotation(SerialisedFormatEntity.class).value());
        }

        @Override
        public Mono<SerialisedFormatEntity> findOneByResourceId(final String resourceId) {
            return this.findById(resourceId);
        }

        @Override
        @NonNull
        public <S extends SerialisedFormatEntity> Mono<S> save(final @NonNull S entity) {
            return this.setOps.add(this.table + KEY_SEP + entity.getSerialisedFormat(), entity.getId())
                    .then(super.saveDefault(entity));
        }

        @Override
        public Flux<SerialisedFormatEntity> findAllBySerialisedFormat(final String serialisedFormat) {
            return this.setOps.members(this.table + KEY_SEP + serialisedFormat)
                    .flatMap(this::findById);
        }

        @Override
        @NonNull
        public Mono<Void> deleteById(final @NonNull String key) {
            return this.findById(key)
                    .map(entity -> this.setOps.remove(this.table + KEY_SEP + entity.getSerialisedFormat()))
                    .then(super.deleteByIdDefault(key));
        }
    }

    /**
     * A class to allow {@link TypeRepository}s to be stored in a reactive repository
     */
    public static class TypeRepositoryAdapter extends ReactiveRepositoryRedisAdapter<TypeEntity, String> implements TypeRepository {

        /**
         * {@link TypeRepositoryAdapter} constructor that takes a redis template
         *
         * @param redisTemplate a {@link ReactiveRedisTemplate} with key of type {@link String} and value of type {@link TypeEntity}
         */
        public TypeRepositoryAdapter(final ReactiveRedisTemplate<String, TypeEntity> redisTemplate) {
            super(redisTemplate, reflectTableAnnotation(TypeEntity.class).value());
        }

        @Override
        public Mono<TypeEntity> findOneByResourceId(final String resourceId) {
            return this.findById(resourceId);
        }

        @Override
        @NonNull
        public <S extends TypeEntity> Mono<S> save(final @NonNull S entity) {
            return this.setOps.add(this.table + KEY_SEP + entity.getType(), entity.getId())
                    .then(super.saveDefault(entity));
        }

        @Override
        public Flux<TypeEntity> findAllByType(final String type) {
            return this.setOps.members(this.table + KEY_SEP + type)
                    .flatMap(this::findById);
        }

        @Override
        @NonNull
        public Mono<Void> deleteById(final @NonNull String key) {
            return this.findById(key)
                    .map(entity -> this.setOps.remove(this.table + KEY_SEP + entity.getType()))
                    .then(super.deleteByIdDefault(key));
        }
    }
}
