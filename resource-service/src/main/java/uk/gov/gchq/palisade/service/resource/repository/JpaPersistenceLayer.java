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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.ParentResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.resource.domain.ResourceEntity;
import uk.gov.gchq.palisade.service.resource.domain.SerialisedFormatEntity;
import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class JpaPersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaPersistenceLayer.class);

    private final ResourceRepository resourceRepository;
    private final TypeRepository typeRepository;
    private final SerialisedFormatRepository serialisedFormatRepository;

    public JpaPersistenceLayer(final ResourceRepository resourceRepository, final TypeRepository typeRepository, final SerialisedFormatRepository serialisedFormatRepository) {
        this.resourceRepository = requireNonNull(resourceRepository, "ResourceRepository cannot be null");
        this.typeRepository = requireNonNull(typeRepository, "TypeRepository cannot be null");
        this.serialisedFormatRepository = requireNonNull(serialisedFormatRepository, "SerialisedFormatRepository cannot be null");
    }

    private Optional<Resource> getResourceById(final String resourceId) {
        return resourceRepository.findByResourceId(resourceId).map(ResourceEntity::getResource);
    }

    // Test on recursionPredicate applied after callback consume
    // Simply prepend predicate to callback to change to opposite behaviour
    private <T extends Resource> T traverseParentsByEntity(final T resource, final BiConsumer<ParentResource, ChildResource> callback, final BiPredicate<ParentResource, ChildResource> recursionPredicate) {
        if (resource instanceof ChildResource) {
            // Treat resource as a ChildResource
            ChildResource childResource = (ChildResource) resource;
            ResourceEntity childEntity = resourceRepository.findByResourceId(childResource.getId())
                    .orElseThrow(() -> new NoSuchElementException("Could not find resource entity for resource " + childResource.getId()));
            // Get the parent
            ResourceEntity parentEntity = resourceRepository.findByResourceId(childEntity.getParentId())
                    .orElseThrow(() -> new NoSuchElementException("Could not find resource entity for resource " + childEntity.getParentId()));
            ParentResource parentResource = (ParentResource) parentEntity.getResource();
            // Apply the callback
            callback.accept(parentResource, childResource);
            // Recurse if desired
            if (recursionPredicate.test(parentResource, childResource)) {
                traverseParentsByEntity(parentResource, callback, recursionPredicate);
            }
        }
        return resource;
    }

    private <T extends Resource> T traverseParentsByResource(final T resource, final BiConsumer<ParentResource, ChildResource> callback, final BiPredicate<ParentResource, ChildResource> recursionPredicate) {
        if (resource instanceof ChildResource) {
            // Treat resource as a ChildResource
            ChildResource childResource = (ChildResource) resource;
            ParentResource parentResource = childResource.getParent();
            // Apply the callback
            callback.accept(parentResource, childResource);
            // Recurse if desired
            if (recursionPredicate.test(parentResource, childResource)) {
                traverseParentsByResource(parentResource, callback, recursionPredicate);
            }
        }
        return resource;
    }

    private Stream<LeafResource> collectLeaves(final Resource resource) {
        if (resource instanceof ParentResource) {
            // Treat resource as a ParentResource
            ParentResource parentResource = (ParentResource) resource;
            // Get the children
            Stream<ResourceEntity> childEntities = resourceRepository.findAllByParentId(parentResource.getId());
            Stream<Resource> childResources = childEntities
                    .map(ResourceEntity::getResource);
            // Recurse
            return childResources.flatMap(this::collectLeaves);
        } else {
            LeafResource leafResource = (LeafResource) resource;
            return Stream.of(leafResource);
        }
    }

    private <T extends Resource> T resolveParents(final T childResource) {
        return traverseParentsByEntity(childResource, (parent, child) -> child.setParent(parent), (parent, child) -> true);
    }

    private <T extends Resource> T resolveParentsUpto(final T childResource, final Resource rootResource) {
        return traverseParentsByEntity(childResource, (parent, child) -> child.setParent(parent), (parent, child) -> !(parent.getId().equals(rootResource.getId()) || child.getId().equals(rootResource.getId())));
    }

    /**
     * In order for this stream to be managed and closed appropriately, methods querying must be {@link Transactional}
      */
    @Override
    public Optional<Stream<LeafResource>> getResourcesById(final String resourceId) {
        LOGGER.debug("Getting resources by id {}", resourceId);
        return resourceRepository.findByResourceId(resourceId)
                .map(ResourceEntity::getResource)
                .map(rootResource -> collectLeaves(rootResource)
                        .map(leafResource -> resolveParentsUpto(leafResource, rootResource)));
    }

    /**
     * In order for this stream to be managed and closed appropriately, methods querying must be {@link Transactional}
     */
    @Override
    public Optional<Stream<LeafResource>> getResourcesByType(final String type) {
        LOGGER.debug("Getting resources by type {}", type);
        // Note that there is no differentiation between having a persisted emptySet and having no persisted entries
        // As such, getResourcesByType("some completely made-up type") will always call the real resource-service and will never be persisted
        return typeRepository.existsByType(type)
                ? Optional.of(
                        typeRepository.findAllByType(type)
                            .map(TypeEntity::getResourceId)
                            .map(this::getResourceById)
                            .map(optResource -> optResource
                                .orElseThrow(() -> new NoSuchElementException("Could not find resource entity for resource while traversing type " + type)))
                            .map(resource -> resolveParents((LeafResource) resource)))
                : Optional.empty();
    }

    /**
     * In order for this stream to be managed and closed appropriately, methods querying must be {@link Transactional}
     */
    @Override
    public Optional<Stream<LeafResource>> getResourcesBySerialisedFormat(final String serialisedFormat) {
        LOGGER.debug("Getting resources by serialised format {}", serialisedFormat);
        // Note that there is no differentiation between having a persisted emptySet and having no persisted entries
        // As such, getResourcesBySerialisedFormat("some completely made-up format") will always call the real resource-service and will never be persisted
        return serialisedFormatRepository.existsBySerialisedFormat(serialisedFormat)
                ? Optional.of(
                        serialisedFormatRepository.findAllBySerialisedFormat(serialisedFormat)
                                .map(SerialisedFormatEntity::getResourceId)
                                .map(this::getResourceById)
                                .map(optResource -> optResource
                                        .orElseThrow(() -> new NoSuchElementException("Could not find resource entity for resource while traversing serialised format " + serialisedFormat)))
                                .map(resource -> resolveParents((LeafResource) resource)))
                : Optional.empty();
    }

    @Transactional
    @Override
    public void putResourcesById(final String rootResourceId, final LeafResource leafResource) {
        LOGGER.debug("Putting resource and parents up-to {} for resource {}", rootResourceId, leafResource);
        // Variable reference must be final inside lambda, but value reference intentionally non-final
        // A bit hacky, but seems to be the best way to have mutable 'globals' inside lambdas
        final Resource[] rootReference = new Resource[1];

        Consumer<ChildResource> saveComplete = child -> resourceRepository.save(new ResourceEntity(leafResource));
        BiPredicate<ParentResource, ChildResource> recurseToRootId = (parent, child) -> {
            if (parent.getId().equals(rootResourceId)) {
                if (isNull(rootReference[0])) {
                    rootReference[0] = parent;
                }
                return false;
            } else if (child.getId().equals(rootResourceId)) {
                if (isNull(rootReference[0])) {
                    rootReference[0] = child;
                }
                return false;
            } else {
                return true;
            }
        };
        Predicate<Resource> isPersistedAndComplete = resource -> resourceRepository.findByResourceId(resource.getId()).map(ResourceEntity::isComplete).orElse(false);
        // Persist this leaf and the collection of its parents as a complete set up to the root resource, overwriting existing entries
        traverseParentsByResource(leafResource, (parent, child) -> saveComplete.accept(child), recurseToRootId.or((parent, child) -> isPersistedAndComplete.test(parent)));

        // If the root reference was found (ie. if there exist directories above this leaf that have been persisted)
        Optional.ofNullable(rootReference[0]).ifPresent(rootResource -> {
            // Edge-case to persist the root resource of the request/response only if it was not persisted as one of the LeafResource/ConnectionDetail elements of the above
            if (!(rootResource instanceof LeafResource)) {
                resourceRepository.save(new ResourceEntity(rootResource));
            }

            Consumer<ParentResource> saveIncomplete = parent -> {
                ResourceEntity entity = new ResourceEntity(parent);
                entity.setComplete(false);
                resourceRepository.save(entity);
            };
            Consumer<ParentResource> saveNewIncomplete = parent -> {
                if (!isPersistedAndComplete.test(parent)) {
                    saveIncomplete.accept(parent);
                }
            };
            BiPredicate<ParentResource, ChildResource> recurseEverything = (parent, child) -> true;
            // Persist higher parents as incomplete once above the root resource, but don't overwrite existing parents that may already be complete
            traverseParentsByResource(rootResource, (parent, child) -> saveNewIncomplete.accept(parent), recurseEverything);
        });
    }

    @Transactional
    @Override
    public void putResourcesByType(final String type, final LeafResource leafResource) {
        LOGGER.debug("Putting resource for type {} for resource {}", type, leafResource);
        putResourcesById(leafResource.getId(), leafResource);
        typeRepository.save(new TypeEntity(type, leafResource.getId()));
    }

    @Transactional
    @Override
    public void putResourcesBySerialisedFormat(final String serialisedFormat, final LeafResource leafResource) {
        LOGGER.debug("Putting resource for serialised format {} for resource {}", serialisedFormat, leafResource);
        putResourcesById(leafResource.getId(), leafResource);
        serialisedFormatRepository.save(new SerialisedFormatEntity(serialisedFormat, leafResource.getId()));
    }

    @Transactional
    @Override
    public void addResource(final LeafResource leafResource) {
        String resourceId = leafResource.getId();
        String type = leafResource.getType();
        String serialisedFormat = leafResource.getSerialisedFormat();

        // Update resources repository
        LOGGER.debug("Adding resourceId {} for resource {}", resourceId, leafResource);
        putResourcesById(resourceId, leafResource);

        // Update type repository (don't use above method, avoid unnecessary double-add)
        if (getResourcesByType(type).isPresent()) {
            LOGGER.debug("Adding type {} for resource {}", type, leafResource);
            typeRepository.save(new TypeEntity(type, resourceId));
        } else {
            LOGGER.debug("Skipping add for type {} as entry not present (partial store would be incomplete)", type);
        }

        // Update serialisedFormat repository (don't use above method, avoid unnecessary double-add)
        if (getResourcesBySerialisedFormat(serialisedFormat).isPresent()) {
            LOGGER.debug("Adding serialised format {} for resource {}", serialisedFormat, leafResource);
            serialisedFormatRepository.save(new SerialisedFormatEntity(serialisedFormat, resourceId));
        } else {
            LOGGER.debug("Skipping add for serialised format {} as entry not present (partial store would be incomplete)", serialisedFormat);
        }
    }

}
