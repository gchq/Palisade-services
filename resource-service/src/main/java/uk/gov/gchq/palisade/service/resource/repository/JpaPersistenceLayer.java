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

import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.ParentResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.resource.domain.EntityType;
import uk.gov.gchq.palisade.service.resource.domain.OrphanedChildJsonMixin;
import uk.gov.gchq.palisade.service.resource.domain.ResourceEntity;
import uk.gov.gchq.palisade.service.resource.domain.SerialisedFormatEntity;
import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class JpaPersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaPersistenceLayer.class);
    private static final String COMPLETE = "complete";
    private static final String NOT_COMPLETE = "not complete";

    private final CompletenessRepository completenessRepository;
    private final ResourceRepository resourceRepository;
    private final TypeRepository typeRepository;
    private final SerialisedFormatRepository serialisedFormatRepository;

    public JpaPersistenceLayer(final CompletenessRepository completenessRepository, final ResourceRepository resourceRepository, final TypeRepository typeRepository, final SerialisedFormatRepository serialisedFormatRepository) {
        this.completenessRepository = requireNonNull(completenessRepository, "CompletenessRepository cannot be null");
        this.resourceRepository = requireNonNull(resourceRepository, "ResourceRepository cannot be null");
        this.typeRepository = requireNonNull(typeRepository, "TypeRepository cannot be null");
        this.serialisedFormatRepository = requireNonNull(serialisedFormatRepository, "SerialisedFormatRepository cannot be null");
    }

    // ~~~ A large number of helper methods for safely manipulating the various repositories ~~~ //

    /**
     * Predicate factory for binding the root resource id and root reference to the lambda BiPredicate
     * The root reference is a one-element list, which upon the predicate returning 'false' will contain (a reference to) the root resource
     * as specified by id
     * This saves doing a lot of unnecessary db lookups for an entity we just recursed over
     * This is exclusively used as something passed to the traverseParents methods
     *
     * @param rootResourceId resourceId for end-of-recursion
     * @param rootReference  an {@link AtomicReference} to set to the root resource if found by this method
     * @return a {@link BiPredicate} that returns false if either argument matches the rootResourceId
     */
    private BiPredicate<ParentResource, ChildResource> recurseToRootId(final String rootResourceId, final AtomicReference<Resource> rootReference) {
        // Return a predicate with the method arguments bound to the lambda
        return (parent, child) -> {
            LOGGER.debug("Looking for root '{}'", rootResourceId);
            if (parent.getId().equals(rootResourceId)) {
                // If the parent is the root resource
                // Set the reference appropriately and halt recursion
                rootReference.set(parent);
                LOGGER.debug("Stop traverse, parent is root '{}'", rootResourceId);
                return false;
            } else if (child.getId().equals(rootResourceId)) {
                // If the child is the root resource
                // Set the reference appropriately and halt recursion
                rootReference.set(child);
                LOGGER.debug("Stop traverse, child is root '{}'", rootResourceId);
                return false;
            } else {
                // Neither parent nor child are the root resource, continue recursion
                LOGGER.debug("Recurse traverse, child is {} and parent is {}", child.getId(), parent.getId());
                return true;
            }
        };
    }

    /**
     * Predicate to determine whether or not a resource is complete
     * Note that complete resource does not necessarily imply a persisted resource, in the case of empty streams for directories by id
     * Since only returned leaf resources can give info on parents, parent resources with no leaf resource children may be complete but
     * not persisted (what type of ParentResource is it?)
     *
     * @param resourceId the resource to get from the completeness repository
     * @return true if the resource was in the repository
     */
    private boolean isResourceIdComplete(final String resourceId) {
        // Check details with completeness db
        boolean complete = completenessRepository.compositeExistsByEntityTypeAndEntityId(EntityType.RESOURCE, resourceId);
        LOGGER.debug("Resource {} is {}", resourceId, complete ? COMPLETE : NOT_COMPLETE);
        return complete;
    }

    /**
     * Predicate to determine whether or not a type is complete
     *
     * @param type the resource type to get from the completeness repository
     * @return true if the type was in the repository
     */
    private boolean isTypeComplete(final String type) {
        // Check details with completeness db
        boolean complete = completenessRepository.compositeExistsByEntityTypeAndEntityId(EntityType.TYPE, type);
        LOGGER.info("Type {} is {}", type, complete ? COMPLETE : NOT_COMPLETE);
        return complete;
    }

    /**
     * Predicate to determine whether or not a serialisedFormat is complete
     *
     * @param serialisedFormat the resource serialised format to get from the completeness repository
     * @return true if the serialised format was in the repository
     */
    private boolean isSerialisedFormatComplete(final String serialisedFormat) {
        // Check details with completeness db
        boolean complete = completenessRepository.compositeExistsByEntityTypeAndEntityId(EntityType.FORMAT, serialisedFormat);
        LOGGER.debug("SerialisedFormat {} is {}", serialisedFormat, complete ? COMPLETE : NOT_COMPLETE);
        return complete;
    }

    /**
     * Predicate to determine whether or not a resource is persisted
     * ie. It is present in persistence, regardless of completeness
     *
     * @param resourceId the resource to get from the resources repository
     * @return true if the resource was in the repository
     */
    private boolean isResourceIdPersisted(final String resourceId) {
        // Get entity from db
        boolean persisted = resourceRepository.findByResourceId(resourceId).iterator()
                // Return whether or not such an entity exists
                .hasNext();
        LOGGER.debug("Resource {} is {}", resourceId, persisted ? COMPLETE : NOT_COMPLETE);
        return persisted;
    }

    /**
     * Iterate over each parent-child pair, with the parent resolved by querying persistence
     * Apply the callback predicate to each parent-child pair
     * Stop if the predicate is not satisfied or if no further parents exist
     *
     * @param <T>          the type of this initial resource, e.g. {@link LeafResource}
     * @param resource     the initial resource to begin operating and recursing up from
     * @param callbackPred callback function to apply to each parent-child pair, return false to stop recursion
     * @return the resource passed as the first argument, useful for Stream.map operations
     */
    private <T extends Resource> T traverseParentsByEntity(final T resource, final BiPredicate<ParentResource, ChildResource> callbackPred) {
        if (resource instanceof ChildResource) {
            // Treat resource as a ChildResource
            ChildResource childResource = (ChildResource) resource;
            ResourceEntity childEntity = resourceRepository.findByResourceId(childResource.getId()).iterator().next();
            // Get the parent
            ResourceEntity parentEntity = resourceRepository.findByResourceId(childEntity.getParentId()).iterator().next();
            ParentResource parentResource = (ParentResource) parentEntity.getResource();
            // Recurse if desired
            if (callbackPred.test(parentResource, childResource)) {
                traverseParentsByEntity(parentResource, callbackPred);
            }
        }
        // Nice for transparency, even though the reference has been mutated
        return resource;
    }

    /**
     * Iterate over each parent-child pair, with the parent resolved by getting from the child resource
     * Apply the callback predicate to each parent-child pair
     * Stop if the predicate is not satisfied or if no further parents exist
     *
     * @param resource     the initial resource to begin operating and recursing up from
     * @param callbackPred callback function to apply to each parent-child pair, return false to stop recursion
     */
    private void traverseParentsByResource(final Resource resource, final BiPredicate<ParentResource, ChildResource> callbackPred) {
        if (resource instanceof ChildResource) {
            // Treat resource as a ChildResource
            ChildResource childResource = (ChildResource) resource;
            ParentResource parentResource = childResource.getParent();
            // Recurse if desired
            if (callbackPred.test(parentResource, childResource)) {
                traverseParentsByResource(parentResource, callbackPred);
            }
        }
        // Nice for transparency, even though the reference has been mutated
    }

    /**
     * Collect from persistence all {@link LeafResource}s 'underneath' this resource.
     * This may be the resource itself, or all resources that have this as a parent, or grand*parent
     *
     * @param resource the top-level resource to get the leaves of
     * @return a {@link Stream} of {@link LeafResource}s from the resource repository persistence store
     */
    private FunctionalIterator<LeafResource> collectLeaves(final Resource resource) {
        if (resource instanceof ParentResource) {
            // Treat resource as a ParentResource
            ParentResource parentResource = (ParentResource) resource;
            // Get the children
            FunctionalIterator<ResourceEntity> childEntities = resourceRepository.iterateFindAllByParentId(parentResource.getId());
            FunctionalIterator<Resource> childResources = childEntities
                    .map(ResourceEntity::getResource);
            // Recurse over all further children
            return childResources.flatMap(this::collectLeaves)
                    .map(leafResource -> resolveParentsUpto(leafResource, resource));
        } else {
            // If we have reached a leaf, then done
            // Return one-element stream (this could probably lead to some performance penalty)
            // It should be a negligible hit to performance, relative how much other processing happens per-leaf
            return FunctionalIterator.fromIterator(
                    Collections.singletonList((LeafResource) resource).iterator());
        }
    }

    /**
     * Given a resource, get from persistence its grand*parents and set them up appropriately
     * This means setting each {@link ChildResource}'s parent to the appropriate {@link ParentResource}
     * This is effectively undoing the effects of serialisation using the {@link OrphanedChildJsonMixin}
     *
     * @param <T>           the type of this initial resource, e.g. {@link LeafResource}
     * @param childResource the initial resource to recurse up from
     * @return the resource with all parents resolved up-to some 'root' resource, usually the first {@link Resource}
     * that wasn't an instance of {@link ChildResource}
     */
    private <T extends Resource> T resolveParents(final T childResource) {
        return traverseParentsByEntity(
                childResource,
                (parent, child) -> {
                    child.setParent(parent);
                    return true;
                }
        );
    }

    /**
     * Given a resource, get from persistence its grand*parents and set them up appropriately
     * This means setting each {@link ChildResource}'s parent to the appropriate {@link ParentResource}
     * Stop once a given 'root' resource has been reached at either the parent or the child
     * In practice, this check is only satisfied by the child when childResource == rootResource
     * Otherwise it will always be satisfied by the parent first before it is satisfied by the child
     * Subsequently, this is split as one initial check by child and all subsequent checks by parent
     *
     * @param <T>           the type of this initial resource, e.g. {@link LeafResource}
     * @param childResource the initial resource to recurse up from
     * @param rootResource  the resource at which to stop recursion
     * @return the resource with all parents resolved up-to the given rootResource
     */
    private <T extends Resource> T resolveParentsUpto(final T childResource, final Resource rootResource) {
        if (!childResource.getId().equals(rootResource.getId())) {
            return traverseParentsByEntity(
                    childResource,
                    (parent, child) -> {
                        child.setParent(parent);
                        return !parent.getId().equals(rootResource.getId());
                    }
            );
        } else {
            // See traverseParentsByEntity, nice for transparency, even though the reference has been mutated
            return childResource;
        }
    }

    /**
     * Save the given resource to persistence as an incomplete entity
     * ie. there are missing children of this resource
     * This involves just adding to the resource repository but not the completeness repository
     *
     * @param resource the (incomplete) resource to save
     */
    private void saveIncompleteResource(final Resource resource) {
        // Since this is a 'low-quality' set of information, we never want to overwrite a 'high-quality' (complete) entity
        // There is no benefit to overwrite a 'low-quality' (incomplete) entity as it should be equivalent
        // Therefore, if this resource is already persisted, skip
        if (!isResourceIdPersisted(resource.getId())) {
            // Create an entity
            ResourceEntity entity = new ResourceEntity(resource);
            LOGGER.debug("Persistence save for incomplete resource entity '{}' with parent '{}'", entity.getResourceId(), entity.getParentId());
            // Save to db
            resourceRepository.save(entity);
            // Don't save to completeness db
        }
    }

    /**
     * Save the given {@link Resource} to persistence as a complete entity
     * ie. all children of this resource have been or will be persisted
     * Add it to both the completeness and resource repositories
     *
     * @param resource the (complete) resource to save
     */
    private void saveCompleteResource(final Resource resource) {
        // Since this is a 'high-quality' set of information, we always want to overwrite a 'low-quality' (incomplete) entity
        // There is no benefit to overwrite a 'high-quality' (complete) entity as it should be equivalent
        // Therefore, if this resource is already persisted AND complete, skip
        // Otherwise, overwrite
        if (!isResourceIdComplete(resource.getId())) {
            // Mark resource entity as complete
            LOGGER.debug("Persistence save for entity '{}' with id '{}'", EntityType.RESOURCE, resource.getId());
            completenessRepository.save(EntityType.RESOURCE, resource.getId());
        }
        saveIncompleteResource(resource);
    }

    /**
     * Save the given {@link LeafResource} to persistence, and all intermediaries up-to to a 'root' parent resource id
     * Each of these is saved as a complete resource
     * This parent resource id need not be for a {@link ParentResource} - it may be the the id of the leaf given in the second argument
     *
     * @param parentResourceId the id for the (complete) parent
     * @param leafResource     the {@link LeafResource} that will be saved (as well as some of its parents)
     * @return Optional.of the resource represented by the parentResourceId if it was found while recursing
     * Optional.empty if this resourceId was never found
     */
    private Optional<Resource> saveChildrenOfCompleteResource(final String parentResourceId, final LeafResource leafResource) {
        LOGGER.debug("Putting resource and parents up-to '{}' for resource '{}'", parentResourceId, leafResource.getId());
        // A bit hacky, but used to pull the rootResource out of the recurseToRootId BiPredicate, see comments on method
        // This saves doing a lot of unnecessary db lookups for an entity we just recursed over
        final AtomicReference<Resource> parentReference = new AtomicReference<>();
        // Persist this leaf and the collection of its parents as a complete set up to the root resource
        // If a complete resource is found along the way, no need to overwrite it, but continue recursing
        // If an incomplete resource is found, overwrite it as it is now complete
        // This is a 'high-quality' set of information (as it is complete) that the persistence layer will report as 'truth'
        traverseParentsByResource(
                leafResource,
                (parent, child) -> {
                    saveCompleteResource(child);
                    return recurseToRootId(parentResourceId, parentReference).test(parent, child);
                }
        );
        return Optional.ofNullable(parentReference.get());
    }

    /**
     * Save the given resource to persistence, with the initial resource marked as complete, but all further parents marked as incomplete
     *
     * @param resource the resource to save - likely the resource representing the resourceId of a request to the resource-service
     */
    private void saveResourceWithIncompleteParents(final Resource resource) {
        saveCompleteResource(resource);
        // Higher parents are now a 'low-quality' set of information (as it is incomplete) that the persistence layer cannot report as 'truth'
        // It will only be used to rebuild resources when retrieved from persistence
        // Persist higher parents as incomplete once above the root resource, but don't overwrite resources
        // Subsequently, as soon as a persisted resource is found, stop as all of its further parents will also already be persisted
        traverseParentsByResource(
                resource,
                (parent, child) -> {
                    boolean pred = !isResourceIdPersisted(parent.getId());
                    saveIncompleteResource(parent);
                    return pred;
                });
    }

    /**
     * Save the given resource as a member of the collection of the given type
     * Any resource saved by type implies the type is complete, don't worry about marking as such in completeness
     * The completeness by type will be marked appropriately elsewhere
     *
     * @param type         the type of the {@link LeafResource}
     * @param leafResource the resource with an id that will be saved in the type repository
     */
    private void saveType(final String type, final LeafResource leafResource) {
        TypeEntity entity = new TypeEntity(type, leafResource.getId());
        LOGGER.debug("Persistence save for type entity '{}' with type '{}'", entity.getResourceId(), entity.getType());
        typeRepository.save(entity);
    }

    /**
     * Save the given resource as a member of the collection of the given serialised format
     * Any resource saved by serialised format implies the serialised format is complete, don't worry about marking as such in completeness
     * The completeness by serialised format will be marked appropriately elsewhere
     *
     * @param serialisedFormat the serialised format of the {@link LeafResource}
     * @param leafResource     the resource with an id that will be saved in the serialised format repository
     */
    private void saveSerialisedFormat(final String serialisedFormat, final LeafResource leafResource) {
        SerialisedFormatEntity entity = new SerialisedFormatEntity(serialisedFormat, leafResource.getId());
        LOGGER.debug("Persistence save for serialised format entity '{}' with serialised format '{}'", entity.getResourceId(), entity.getSerialisedFormat());
        serialisedFormatRepository.save(entity);
    }

    /**
     * Get a single resource by resource id with all parents resolved - not necessarily a leaf
     *
     * @param resourceId the id of the resource to get
     * @return {@link Iterator} of a {@link Resource} in persistence, Optional.empty if not found
     */
    private FunctionalIterator<LeafResource> getResourceById(final String resourceId) {
        // Get resource entity from db
        FunctionalIterator<LeafResource> resourceIterator = null;
        FunctionalIterator<ResourceEntity> entityIterator = FunctionalIterator.fromIterator(resourceRepository.findByResourceId(resourceId).iterator());
        while (entityIterator.hasNext()) {
            ResourceEntity entity = entityIterator.next();
            resourceIterator = collectLeaves(resolveParents(entity.getResource()));
        }
        return resourceIterator;
    }

    // ~~~ Actual method implementations/overrides for PersistenceLayer interface ~~~ //

    // Given a resource, return all leaf resources underneath it with all parents resolved
    @Override
    public FunctionalIterator<LeafResource> getResourcesById(final String resourceId) {
        LOGGER.debug("Getting resources by id '{}'", resourceId);
        FunctionalIterator<LeafResource> resourceIterator = FunctionalIterator.fromIterator(Collections.emptyIterator());
        // Only return info on complete sets of information
        if (isResourceIdComplete(resourceId)) {
            LOGGER.info("Persistence hit for resourceId '{}'", resourceId);
            // Get resource entity from db
            FunctionalIterator<ResourceEntity> entityIterator = FunctionalIterator.fromIterator(resourceRepository.findByResourceId(resourceId).iterator());
            while (entityIterator.hasNext()) {
                ResourceEntity entity = entityIterator.next();
                FunctionalIterator<LeafResource> iterator = getResourceById(entity.getResourceId());
                if (iterator != null) {
                    resourceIterator = FunctionalIterator.fromIterator(Iterators.concat(resourceIterator, iterator));
                }
            }
        } else {
            LOGGER.info("Persistence miss for resourceId '{}'", resourceId);
            // The persistence store has nothing stored for this resource id, or the store is incomplete
        }
        return resourceIterator;
    }

    // Given a type, return all leaf resources of that type with all parents resolved
    @Override
    public FunctionalIterator<LeafResource> getResourcesByType(final String type) {
        LOGGER.debug("Getting resources by type '{}'", type);
        FunctionalIterator<LeafResource> resourceIterator = FunctionalIterator.fromIterator(Collections.emptyIterator());
        // Only return info on complete sets of information
        if (isTypeComplete(type)) {
            LOGGER.info("Persistence hit for type '{}'", type);
            // Get resource entity from db
            FunctionalIterator<TypeEntity> typeIterator = typeRepository.iterateFindAllByType(type);
            while (typeIterator.hasNext()) {
                TypeEntity entity = typeIterator.next();
                FunctionalIterator<LeafResource> iterator = getResourceById(entity.getResourceId());
                if (iterator != null) {
                    resourceIterator = FunctionalIterator.fromIterator(Iterators.concat(resourceIterator, iterator));
                }
            }
        } else {
            LOGGER.info("Persistence miss for type '{}'", type);
            // The persistence store has nothing stored for this resource id, or the store is incomplete
        }
        return resourceIterator;
    }

    // Given a serialisedFormat, return all leaf resources of that serialisedFormat with all parents resolved
    @Override
    public FunctionalIterator<LeafResource> getResourcesBySerialisedFormat(final String serialisedFormat) {
        LOGGER.debug("Getting resources by serialised format '{}'", serialisedFormat);
        FunctionalIterator<LeafResource> resourceIterator = FunctionalIterator.fromIterator(Collections.emptyIterator());
        // Only return info on complete sets of information
        if (isSerialisedFormatComplete(serialisedFormat)) {
            LOGGER.info("Persistence hit for serialised format '{}'", serialisedFormat);
            // Get resource entity from db
            FunctionalIterator<SerialisedFormatEntity> typeIterator = serialisedFormatRepository.iterateFindAllBySerialisedFormat(serialisedFormat);
            while (typeIterator.hasNext()) {
                SerialisedFormatEntity entity = typeIterator.next();
                FunctionalIterator<LeafResource> iterator = getResourceById(entity.getResourceId());
                if (iterator != null) {
                    resourceIterator = FunctionalIterator.fromIterator(Iterators.concat(resourceIterator, iterator));
                }
            }
        } else {
            LOGGER.info("Persistence miss for serialised format '{}'", serialisedFormat);
            // The persistence store has nothing stored for this resource id, or the store is incomplete
        }
        return resourceIterator;
    }

    // Add a leaf resource and mark it and its parents as complete up to a given root resource id
    // Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
    @Override
    public FunctionalIterator<LeafResource> withPersistenceById(final String rootResourceId, final FunctionalIterator<LeafResource> resources) {
        LOGGER.info("Persistence add for resources by id '{}'", rootResourceId);
        // Persist that this resource id has (a potentially empty stream of) persisted info
        // Next time it is requested, it will be handled by persistence
        completenessRepository.save(EntityType.RESOURCE, rootResourceId);

        final AtomicBoolean persistedRootAndParents = new AtomicBoolean(false);
        return resources.peek(leafResource -> {
            // Persist each leaf resource, with each being complete up-to the root resource id
            Optional<Resource> rootResource = saveChildrenOfCompleteResource(rootResourceId, leafResource);
            // Persist the root resource and its parents
            rootResource.ifPresentOrElse(
                    // If the root reference was found (ie. the leafResource had a grand*parent with id matching the root resource id)
                    // Then persist the root as the final complete entity, with all further parents marked as incomplete
                    resource -> {
                        // This only needs to be done once per withPersistenceById call
                        if (persistedRootAndParents.compareAndSet(false, true)) {
                            saveResourceWithIncompleteParents(resource);
                        }
                    },
                    // Otherwise... not sure?
                    () -> {
                        LOGGER.warn("Putting resource {} into resource repository never led to traversal of the apparent root {} for this put update", leafResource.getId(), rootResourceId);
                        LOGGER.warn("This erroneous behaviour may be a sign of database corruption and should be investigated, but hasn't caused a critical error yet");
                    });
        });
    }

    // Add a leaf resource, (mark the leaf as complete,) and mark the leaf as a given type
    // Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
    @Override
    public FunctionalIterator<LeafResource> withPersistenceByType(final String type, final FunctionalIterator<LeafResource> resources) {
        LOGGER.info("Persistence add for resources by type '{}'", type);
        // Persist that this type has (a potentially empty stream of) persisted info
        // Next time it is requested, it will be handled by persistence
        completenessRepository.save(EntityType.TYPE, type);
        return resources.peek(leafResource -> {
            saveResourceWithIncompleteParents(leafResource);
            saveType(type, leafResource);
        });
    }

    // Add a leaf resource, (mark the leaf as complete,) and mark the leaf as a given serialisedFormat
    // Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
    @Override
    public FunctionalIterator<LeafResource> withPersistenceBySerialisedFormat(final String serialisedFormat, final FunctionalIterator<LeafResource> resources) {
        LOGGER.info("Persistence add for resources by serialisedFormat '{}'", serialisedFormat);
        // Persist that this serialisedFormat has (a potentially empty stream of) persisted info
        // Next time it is requested, it will be handled by persistence
        completenessRepository.save(EntityType.FORMAT, serialisedFormat);
        return resources.peek(leafResource -> {
            saveResourceWithIncompleteParents(leafResource);
            saveSerialisedFormat(serialisedFormat, leafResource);
        });
    }

    // Add a new resource that has been created during runtime to the persistence store
    // Used for updating the persistence store when the source of 'truth' has changed
    // ie. a resource was added to the real resource-service
    // For each repository, if there exists a complete set of information appropriate to this leaf resource, add the resource to that set
    // Appropriate here means:
    // * resource repository: a grand*parent of the leaf is complete, otherwise use the leaf as the 'root'
    // * type repository:     there exist entries for this leaf's type
    // * ser.For. repository: there exist entries for this leaf's serialisedFormat
    //
    // As long as this is called for every new resource created and added to the resource-service,
    // this guarantees consistency between persistence and resource-service
    @Override
    public void addResource(final LeafResource leafResource) {
        LOGGER.debug("Persistence update for new leafResource with resourceId {}", leafResource.getId());
        // Update resources repository
        Optional<Resource> firstCompleteAncestor = Optional.empty();
        // Find the first direct ancestor (grand*parent) that is persisted and complete
        // This informs us how much of the tree needs updating
        for (Resource ancestor = leafResource; ancestor instanceof ChildResource; ancestor = ((ChildResource) ancestor).getParent()) {
            if (isResourceIdComplete(ancestor.getId()) && isResourceIdPersisted(ancestor.getId())) {
                firstCompleteAncestor = Optional.of(ancestor);
                break;
            }
        }
        // Treat this ancestor as the 'root' resource for a putResourcesById (if such an ancestor was found)
        // Otherwise we can use this leafResource and mark just that as complete
        // Useful for when there is persisted information about the resource's type
        Resource effectiveRoot = firstCompleteAncestor.orElse(leafResource);
        LOGGER.debug("Adding resource '{}' with effective root '{}'", leafResource.getId(), effectiveRoot.getId());
        saveResourceWithIncompleteParents(leafResource);

        // Update type repository if there is already a complete set of info
        String type = leafResource.getType();
        if (isTypeComplete(type)) {
            saveType(type, leafResource);
        } else {
            LOGGER.debug("Skipping add for type {} as entry not present (partial store would be incomplete)", type);
        }

        // Update serialisedFormat repository if there is already a complete set of info
        String serialisedFormat = leafResource.getSerialisedFormat();
        if (isSerialisedFormatComplete(serialisedFormat)) {
            saveSerialisedFormat(serialisedFormat, leafResource);
        } else {
            LOGGER.debug("Skipping add for serialised format {} as entry not present (partial store would be incomplete)", serialisedFormat);
        }
    }

}
