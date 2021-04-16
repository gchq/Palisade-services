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

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.resource.common.resource.ChildResource;
import uk.gov.gchq.palisade.service.resource.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.resource.common.resource.ParentResource;
import uk.gov.gchq.palisade.service.resource.common.resource.Resource;
import uk.gov.gchq.palisade.service.resource.domain.EntityType;
import uk.gov.gchq.palisade.service.resource.domain.OrphanedChildJsonMixin;
import uk.gov.gchq.palisade.service.resource.domain.ResourceEntity;
import uk.gov.gchq.palisade.service.resource.domain.SerialisedFormatEntity;
import uk.gov.gchq.palisade.service.resource.domain.TypeEntity;
import uk.gov.gchq.palisade.service.resource.exception.UnknownResourceTypeException;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A class that allows the Resource Service to reactively interact with the persistence backing store
 */
public class ReactivePersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactivePersistenceLayer.class);
    private static final int PARALLELISM = 1;
    private static final String RESOURCE_IS = "Resource '{}' is {}";
    private static final String TYPE_IS = "Type '{}' is {}";
    private static final String FORMAT_IS = "SerialisedFormat '{}' is {}";
    private static final String COMPLETE = "complete";
    private static final String NOT_COMPLETE = "not complete";

    private final CompletenessRepository completenessRepository;
    private final ResourceRepository resourceRepository;
    private final TypeRepository typeRepository;
    private final SerialisedFormatRepository serialisedFormatRepository;

    /**
     * Constructor for ReactivePersistenceLayer
     *
     * @param completenessRepository     the completeness repository
     * @param resourceRepository         the resource repository
     * @param typeRepository             the type repository
     * @param serialisedFormatRepository the serialisedFormat repository
     */
    public ReactivePersistenceLayer(final CompletenessRepository completenessRepository, final ResourceRepository resourceRepository,
                                    final TypeRepository typeRepository, final SerialisedFormatRepository serialisedFormatRepository) {
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
     * @return a {@link BiFunction} that returns false if either argument matches the rootResourceId
     */
    private static BiFunction<ParentResource, ChildResource, CompletableFuture<Boolean>> recurseToRootId(final String rootResourceId, final AtomicReference<Resource> rootReference) {
        // Return a predicate with the method arguments bound to the lambda
        return (ParentResource parent, ChildResource child) -> {
            LOGGER.debug("Looking for root '{}'", rootResourceId);
            if (parent.getId().equals(rootResourceId)) {
                // If the parent is the root resource
                // Set the reference appropriately and halt recursion
                rootReference.set(parent);
                LOGGER.debug("Stop traverse, parent is root '{}'", rootResourceId);
                return CompletableFuture.completedFuture(false);
            } else if (child.getId().equals(rootResourceId)) {
                // If the child is the root resource
                // Set the reference appropriately and halt recursion
                rootReference.set(child);
                LOGGER.debug("Stop traverse, child is root '{}'", rootResourceId);
                return CompletableFuture.completedFuture(false);
            } else {
                // Neither parent nor child are the root resource, continue recursion
                LOGGER.debug("Recurse traverse, child is {} and parent is {}", child.getId(), parent.getId());
                return CompletableFuture.completedFuture(true);
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
     * @return a {@link CompletableFuture} of true if the resource is complete
     */
    private CompletableFuture<Boolean> isResourceIdComplete(final String resourceId) {
        // Check details with completeness db
        return completenessRepository.futureExistsByEntityTypeAndEntityId(EntityType.RESOURCE, resourceId)
                .thenApply((Boolean isResourceComplete) -> {
                    if (isResourceComplete.equals(Boolean.TRUE)) {
                        LOGGER.debug(RESOURCE_IS, resourceId, COMPLETE);
                    } else {
                        LOGGER.debug(RESOURCE_IS, resourceId, NOT_COMPLETE);
                    }
                    return isResourceComplete;
                });
    }

    /**
     * Predicate to determine whether or not a type is complete
     *
     * @param type the resource type to get from the completeness repository
     * @return a {@link CompletableFuture} of true if the type is complete
     */
    private CompletableFuture<Boolean> isTypeComplete(final String type) {
        // Check details with completeness db
        return completenessRepository.futureExistsByEntityTypeAndEntityId(EntityType.TYPE, type)
                .thenApply((Boolean isTypeComplete) -> {
                    if (isTypeComplete.equals(Boolean.TRUE)) {
                        LOGGER.debug(TYPE_IS, type, COMPLETE);
                    } else {
                        LOGGER.debug(TYPE_IS, type, NOT_COMPLETE);
                    }
                    return isTypeComplete;
                });
    }

    /**
     * Predicate to determine whether or not a serialisedFormat is complete
     *
     * @param serialisedFormat the resource serialised format to get from the completeness repository
     * @return a {@link CompletableFuture} of true if the serialisedFormat is complete
     */
    private CompletableFuture<Boolean> isSerialisedFormatComplete(final String serialisedFormat) {
        // Check details with completeness db
        return completenessRepository.futureExistsByEntityTypeAndEntityId(EntityType.FORMAT, serialisedFormat)
                .thenApply((Boolean isFormatComplete) -> {
                    if (isFormatComplete.equals(Boolean.TRUE)) {
                        LOGGER.debug(FORMAT_IS, serialisedFormat, COMPLETE);
                    } else {
                        LOGGER.debug(FORMAT_IS, serialisedFormat, NOT_COMPLETE);
                    }
                    return isFormatComplete;
                });
    }

    /**
     * Predicate to determine whether or not a resource is persisted
     * ie. It is present in persistence, regardless of completeness
     *
     * @param resourceId the resource to get from the resources repository
     * @return a {@link CompletableFuture} of true if the resource was in the repository
     */
    private CompletableFuture<Boolean> isResourceIdPersisted(final String resourceId) {
        // Get entity from db
        return resourceRepository.futureExistsByResourceId(resourceId)
                .thenApply((Boolean persisted) -> {
                    if (persisted.equals(Boolean.TRUE)) {
                        LOGGER.debug(RESOURCE_IS, resourceId, "persisted");
                    } else {
                        LOGGER.debug(RESOURCE_IS, resourceId, "not persisted");
                    }
                    return persisted;
                });

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
    private <T extends Resource> CompletableFuture<Void> traverseParentsByEntity(final T resource, final BiPredicate<ParentResource, ChildResource> callbackPred) {
        if (resource instanceof ChildResource) {
            LOGGER.debug("Traversing child '{}' until predicate is not satisfied", resource.getId());
            // Treat resource as a ChildResource
            ChildResource childResource = (ChildResource) resource;
            LOGGER.debug("Getting ResourceEntity for childResource '{}'", childResource.getId());
            return resourceRepository.findOneByResourceId(childResource.getId()).toFuture()
                    .thenCompose((ResourceEntity childEntity) -> {
                        LOGGER.debug("Getting ResourceEntity for parentResource '{}'", childEntity.getParentId());
                        return resourceRepository.findOneByResourceId(childEntity.getParentId()).toFuture();
                    })
                    .thenCompose((ResourceEntity parentEntity) -> {
                        ParentResource parentResource = (ParentResource) parentEntity.getResource();
                        if (callbackPred.test(parentResource, childResource)) {
                            LOGGER.debug("Pair '{}' and '{}' satisfied predicate, recursing", parentResource.getId(), childResource.getId());
                            return traverseParentsByEntity(parentResource, callbackPred);
                        } else {
                            return CompletableFuture.completedFuture(null);
                        }
                    });
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Iterate over each parent-child pair, with the parent resolved by getting from the child resource
     * Apply the callback function to each parent-child pair
     * Stop if the predicate is not satisfied or if no further parents exist
     *
     * @param resource         the initial resource to begin operating and recursing up from
     * @param callbackFunction callback function to apply to each parent-child pair, return false to stop recursion
     * @return a {@link CompletableFuture} of type {@link Void}
     */
    private static CompletableFuture<Void> traverseParentsByResource(final Resource resource,
                                                                     final BiFunction<ParentResource, ChildResource, CompletableFuture<Boolean>> callbackFunction) {
        if (resource instanceof ChildResource) {
            // Treat resource as a ChildResource
            ChildResource childResource = (ChildResource) resource;
            ParentResource parentResource = childResource.getParent();
            // Recurse if desired
            return callbackFunction.apply(parentResource, childResource)
                    .thenCompose((Boolean shouldRecurse) -> {
                        if (shouldRecurse.equals(Boolean.TRUE)) {
                            return traverseParentsByResource(parentResource, callbackFunction);
                        } else {
                            return CompletableFuture.completedFuture(null);
                        }
                    });
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Collect from persistence all {@link LeafResource}s 'underneath' this resource.
     * This may be the resource itself, or all resources that have this as a parent, or grand*parent
     *
     * @param resource the top-level resource to get the leaves of
     * @return a {@link Stream} of {@link LeafResource}s from the resource repository persistence store
     */
    private Source<LeafResource, NotUsed> collectLeaves(final Resource resource) {
        if (resource instanceof ParentResource) {
            // Treat resource as a ParentResource
            ParentResource parentResource = (ParentResource) resource;
            // Get the children
            return resourceRepository.streamFindAllByParentId(parentResource.getId())
                    .map(ResourceEntity::getResource)
                    // Recurse over further children
                    .flatMapConcat(this::collectLeaves)
                    .mapAsync(PARALLELISM, leafResource -> resolveParentsUpto(leafResource, resource));
        } else if (resource instanceof LeafResource) {
            // If we have reached a leaf, then done
            return Source.single((LeafResource) resource);
        } else {
            throw new UnknownResourceTypeException(String.format("Resource '%s' is neither Parent nor Leaf", resource.getId()));
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
    private <T extends Resource> CompletableFuture<T> resolveParents(final T childResource) {
        return traverseParentsByEntity(
                childResource,
                (ParentResource parent, ChildResource child) -> {
                    child.setParent(parent);
                    return true;
                }
        ).thenApply(ignored -> childResource);
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
    private <T extends Resource> CompletableFuture<T> resolveParentsUpto(final T childResource, final Resource rootResource) {
        if (!childResource.getId().equals(rootResource.getId())) {
            return traverseParentsByEntity(
                    childResource,
                    (ParentResource parent, ChildResource child) -> {
                        if (parent.getId().equals(rootResource.getId())) {
                            if (rootResource instanceof ParentResource) {
                                child.setParent((ParentResource) rootResource);
                            }
                            return false;
                        } else {
                            child.setParent(parent);
                            return true;
                        }
                    }
            ).thenApply(ignored -> childResource);
        } else {
            // See traverseParentsByEntity, nice for transparency, even though the reference has been mutated
            return CompletableFuture.completedFuture(childResource);
        }
    }

    /**
     * Save the given resource to persistence as an incomplete entity
     * ie. there are missing children of this resource
     * This involves just adding to the resource repository but not the completeness repository
     *
     * @param resource the (incomplete) resource to save
     * @return a {@link CompletableFuture} of type {@link Void}
     */
    private CompletableFuture<Void> saveIncompleteResource(final Resource resource) {
        // Since this is a 'low-quality' set of information, we never want to overwrite a 'high-quality' (complete) entity
        // There is no benefit to overwrite a 'low-quality' (incomplete) entity as it should be equivalent
        // Therefore, if this resource is already persisted, skip
        return isResourceIdPersisted(resource.getId())
                .thenCompose((Boolean result) -> {
                    if (result.equals(Boolean.FALSE)) {
                        // Create an entity
                        ResourceEntity entity = new ResourceEntity(resource);
                        // Save to db
                        return resourceRepository.futureSave(entity)
                                .thenRun(() -> LOGGER.debug("Persistence save for incomplete resource entity '{}' with parent '{}'", entity.getResourceId(), entity.getParentId()));
                        // Don't save to completeness db
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    /**
     * Save the given {@link Resource} to persistence as a complete entity
     * ie. all children of this resource have been or will be persisted
     * Add it to both the completeness and resource repositories
     *
     * @param resource the (complete) resource to save
     * @return a {@link CompletableFuture} of type {@link Void}
     */
    private CompletableFuture<Void> saveCompleteResource(final Resource resource) {
        // Since this is a 'high-quality' set of information, we always want to overwrite a 'low-quality' (incomplete) entity
        // There is no benefit to overwrite a 'high-quality' (complete) entity as it should be equivalent
        // Therefore, if this resource is already persisted AND complete, skip
        // Otherwise, overwrite
        return isResourceIdComplete(resource.getId())
                .thenCompose((Boolean result) -> {
                    if (result.equals(Boolean.FALSE)) {
                        // Mark resource entity as complete
                        return completenessRepository.futureSave(EntityType.RESOURCE, resource.getId())
                                .thenRun(() -> LOGGER.debug("Persistence save for complete entity '{}' with id '{}'", EntityType.RESOURCE, resource.getId()));
                    } else {
                        return saveIncompleteResource(resource);
                    }
                });
    }

    /**
     * Save the given {@link LeafResource} to persistence, and all intermediaries up-to to a 'root' parent resource id
     * Each of these is saved as a complete resource
     * This parent resource id need not be for a {@link ParentResource} - it may be the the id of the leaf given in the second argument
     *
     * @param parentResourceId the id for the (complete) parent
     * @param leafResource     the {@link LeafResource} that will be saved (as well as some of its parents)
     * @return a {@link CompletableFuture} of Optional.of the resource represented by the parentResourceId if it was found while recursing
     * Optional.empty if this resourceId was never found
     */
    private CompletableFuture<Optional<Resource>> saveChildrenOfCompleteResource(final String parentResourceId, final Resource leafResource) {
        LOGGER.debug("Putting resource and parents up-to '{}' for resource '{}'", parentResourceId, leafResource.getId());
        // A bit hacky, but used to pull the rootResource out of the recurseToRootId BiPredicate, see comments on method
        // This saves doing a lot of unnecessary db lookups for an entity we just recursed over
        final AtomicReference<Resource> parentReference = new AtomicReference<>();
        // Persist this leaf and the collection of its parents as a complete set up to the root resource
        // If a complete resource is found along the way, no need to overwrite it, but continue recursing
        // If an incomplete resource is found, overwrite it as it is now complete
        // This is a 'high-quality' set of information (as it is complete) that the persistence layer will report as 'truth'
        return traverseParentsByResource(leafResource,
                (ParentResource parent, ChildResource child) -> {
                    saveCompleteResource(child);
                    return recurseToRootId(parentResourceId, parentReference).apply(parent, child);
                }
        ).thenApply(ignored -> Optional.ofNullable(parentReference.get()));
    }

    /**
     * Save the given resource to persistence, with the initial resource marked as complete, but all further parents marked as incomplete
     *
     * @param resource the resource to save - likely the resource representing the resourceId of a request to the resource-service
     * @return a {@link CompletableFuture} of type {@link Void}
     */
    private CompletableFuture<Void> saveResourceWithIncompleteParents(final Resource resource) {
        // Higher parents are now a 'low-quality' set of information (as it is incomplete) that the persistence layer cannot report as 'truth'
        // It will only be used to rebuild resources when retrieved from persistence
        // Persist higher parents as incomplete once above the root resource, but don't overwrite resources
        // Subsequently, as soon as a persisted resource is found, stop as all of its further parents will also already be persisted
        return saveCompleteResource(resource)
                .thenCombine(traverseParentsByResource(resource,
                        (parent, child) -> isResourceIdPersisted(parent.getId())
                                .thenCombine(saveIncompleteResource(parent), (resourceIdPersisted, savedParent) -> !resourceIdPersisted)),
                        (ignored, alsoIgnored) -> null);
    }

    /**
     * Save the given resource as a member of the collection of the given type
     * Any resource saved by type implies the type is complete, don't worry about marking as such in completeness
     * The completeness by type will be marked appropriately elsewhere
     *
     * @param type         the type of the {@link LeafResource}
     * @param leafResource the resource with an id that will be saved in the type repository
     * @return a {@link CompletableFuture} of type {@link Void}
     */
    private CompletableFuture<Void> saveType(final String type, final LeafResource leafResource) {
        return typeRepository.futureExistsByResourceId(leafResource.getId())
                .thenCompose((Boolean alreadySaved) -> {
                    if (alreadySaved.equals(Boolean.FALSE)) {
                        TypeEntity entity = new TypeEntity(type, leafResource.getId());
                        return typeRepository.futureSave(entity)
                                .thenRun(() -> LOGGER.debug("Persistence save for type entity '{}' with type '{}'", entity.getResourceId(), entity.getType()));
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    /**
     * Save the given resource as a member of the collection of the given serialised format
     * Any resource saved by serialised format implies the serialised format is complete, don't worry about marking as such in completeness
     * The completeness by serialised format will be marked appropriately elsewhere
     *
     * @param serialisedFormat the serialised format of the {@link LeafResource}
     * @param leafResource     the resource with an id that will be saved in the serialised format repository
     * @return a {@link CompletableFuture} of type {@link Void}
     */
    private CompletableFuture<Void> saveSerialisedFormat(final String serialisedFormat, final LeafResource leafResource) {
        return serialisedFormatRepository.futureExistsFindOneByResourceId(leafResource.getId())
                .thenCompose((Boolean alreadySaved) -> {
                    if (alreadySaved.equals(Boolean.FALSE)) {
                        SerialisedFormatEntity entity = new SerialisedFormatEntity(serialisedFormat, leafResource.getId());
                        return serialisedFormatRepository.futureSave(entity)
                                .thenRun(() -> LOGGER.debug("Persistence save for type entity '{}' with serialisedFormat '{}'",
                                        entity.getResourceId(), entity.getSerialisedFormat()));
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    /**
     * Get a single resource by resource id with all parents resolved - not necessarily a leaf
     *
     * @param resourceId the id of the resource to get
     * @return {@link FunctionalIterator} of a {@link Resource} in persistence, empty {@link FunctionalIterator} if not found
     */
    private Source<Resource, NotUsed> getResourceById(final String resourceId) {
        // Get resource entity from db
        return resourceRepository.streamFindOneByResourceId(resourceId)
                // Get resource from db entity
                .map(ResourceEntity::getResource)
                // Resolve this resource's parents until no more parents found in db
                .mapAsync(PARALLELISM, this::resolveParents);
    }

    // ~~~ Actual method implementations/overrides for PersistenceLayer interface ~~~ //

    // Given a resource, return all leaf resources underneath it with all parents resolved
    @Override
    public CompletableFuture<Optional<Source<LeafResource, NotUsed>>> getResourcesById(final String resourceId) {
        LOGGER.info("Getting resources by id '{}'", resourceId);
        // Only return info on complete sets of information
        return isResourceIdComplete(resourceId)
                .thenApply((Boolean isIdComplete) -> {
                    if (isIdComplete.equals(Boolean.TRUE)) {
                        LOGGER.info("Persistence hit for resourceId '{}'", resourceId);
                        // Get resource entity from db
                        return Optional.of(resourceRepository.streamFindOneByResourceId(resourceId)
                                // Get resource from db entity
                                .map(ResourceEntity::getResource)
                                // Optimisation - resolve this resource's parents as itself is a parent of each leaf
                                // This means leaves have fewer parents to resolve AND may help with memory usage
                                // Each resource pulled from the database is unique, even for the same entity
                                .mapAsync(PARALLELISM, this::resolveParents)
                                // Get all leaves of this resource with parents resolved up to this resource
                                // See above, all parents are now resolved
                                .flatMapConcat(this::collectLeaves));
                    } else {
                        LOGGER.info("Persistence miss for resourceId '{}'", resourceId);
                        // The persistence store has nothing stored for this resource id, or the store is incomplete
                        return Optional.empty();
                    }
                });
    }

    // Given a type, return all leaf resources of that type with all parents resolved
    @Override
    public CompletableFuture<Optional<Source<LeafResource, NotUsed>>> getResourcesByType(final String type) {
        LOGGER.info("Getting resources by type '{}'", type);
        // Only return info on complete sets of information
        return isTypeComplete(type)
                .thenApply((Boolean isTypeComplete) -> {
                    if (isTypeComplete.equals(Boolean.TRUE)) {
                        LOGGER.info("Persistence hit for type '{}'", type);
                        // Get type entity from db
                        return Optional.of(typeRepository.streamFindAllByType(type)
                                // Get the resourceId from the TypeEntity
                                .map(TypeEntity::getResourceId)
                                // Get the resource for this id
                                .flatMapConcat(this::getResourceById)
                                // Some enforcement of db assumptions
                                // If we have a type entity, we have a resource entity AND the resource is a leaf
                                // May throw ClassCastException or NoSuchElementException if database is malformed
                                .map(LeafResource.class::cast)
                        );
                    } else {
                        LOGGER.info("Persistence miss for type '{}'", type);
                        // The persistence store has nothing stored for this type, or the store is incomplete
                        return Optional.empty();
                    }
                });
    }

    // Given a serialisedFormat, return all leaf resources of that serialisedFormat with all parents resolved
    @Override
    public CompletableFuture<Optional<Source<LeafResource, NotUsed>>> getResourcesBySerialisedFormat(final String serialisedFormat) {
        LOGGER.info("Getting resources by serialisedFormat '{}'", serialisedFormat);
        // Only return info on complete sets of information
        return isSerialisedFormatComplete(serialisedFormat)
                .thenApply((Boolean isFormatComplete) -> {
                    if (isFormatComplete.equals(Boolean.TRUE)) {
                        LOGGER.info("Persistence hit for serialisedFormat '{}'", serialisedFormat);
                        // Get type entity from db
                        return Optional.of(serialisedFormatRepository.streamFindAllBySerialisedFormat(serialisedFormat)
                                // Get the resourceId from the TypeEntity
                                .map(SerialisedFormatEntity::getResourceId)
                                // Get the resource for this id
                                .flatMapConcat(this::getResourceById)
                                // Some enforcement of db assumptions
                                // If we have a type entity, we have a resource entity AND the resource is a leaf
                                // May throw ClassCastException or NoSuchElementException if database is malformed
                                .map(LeafResource.class::cast)
                        );
                    } else {
                        LOGGER.info("Persistence miss for serialisedFormat '{}'", serialisedFormat);
                        // The persistence store has nothing stored for this serialisedFormat, or the store is incomplete
                        return Optional.empty();
                    }
                });
    }

    // Add a leaf resource and mark it and its parents as complete up to a given root resource id
    // Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
    @Override
    public <T extends LeafResource> Flow<T, T, NotUsed> withPersistenceById(final String rootResourceId) {
        LOGGER.info("Persistence add for resources by id '{}'", rootResourceId);
        final AtomicBoolean persistedRootAndParents = new AtomicBoolean(false);
        // Persist that this resource id has (a potentially empty stream of) persisted info
        // Next time it is requested, it will be handled by persistence
        Flow<T, T, NotUsed> flow = Flow.<T>create().mapAsync(PARALLELISM, (T leafResource) ->
                // Persist each leaf resource, with each being complete up-to the root resource id
                saveChildrenOfCompleteResource(rootResourceId, leafResource)
                        // Persist the root resource and its parents
                        .thenCompose(rootResource -> rootResource
                                // If the root reference was found (ie. the leafResource had a grand*parent with id matching the root resource id)
                                // Then persist the root as the final complete entity, with all further parents marked as incomplete
                                .map((Resource resource) -> {
                                    // This only needs to be done once per withPersistenceById call
                                    if (persistedRootAndParents.compareAndSet(false, true)) {
                                        return saveResourceWithIncompleteParents(resource);
                                    } else {
                                        return CompletableFuture.completedFuture((Void) null);
                                    }
                                })
                                // Otherwise... not sure?
                                .orElseGet(() -> {
                                    LOGGER.warn("Putting resource {} into resource repository never led to traversal of the apparent root {} for this put update",
                                            leafResource.getId(), rootResourceId);
                                    LOGGER.warn("This erroneous behaviour may be a sign of database corruption and should be investigated, but hasn't caused a critical error yet");
                                    return CompletableFuture.completedFuture(null);
                                })
                        )
                        .thenApply(ignored -> leafResource)
        );

        return Flow.completionStageFlow(isResourceIdComplete(rootResourceId)
                .thenCompose((Boolean idAlreadyComplete) -> {
                    if (idAlreadyComplete.equals(Boolean.FALSE)) {
                        return completenessRepository.futureSave(EntityType.RESOURCE, rootResourceId).thenApply(ignored -> null);
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                })
                .thenApply(ignored -> flow))
                .mapMaterializedValue(future -> NotUsed.notUsed());
    }

    @Override
    public <T extends LeafResource> Flow<T, T, NotUsed> withPersistenceByType(final String type) {
        LOGGER.info("Persistence add for resources by type '{}'", type);
        // Persist that this type has (a potentially empty stream of) persisted info
        // Next time it is requested, it will be handled by persistence
        return Flow.lazyCompletionStageFlow(() -> isTypeComplete(type)
                .thenCompose((Boolean typeAlreadyComplete) -> {
                    if (typeAlreadyComplete.equals(Boolean.FALSE)) {
                        return completenessRepository.futureSave(EntityType.TYPE, type);
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                })
                .thenApply(ignored -> Flow.<T>create()
                        .mapAsync(PARALLELISM, (T leafResource) -> saveResourceWithIncompleteParents(leafResource)
                                .thenCompose(alsoIgnored -> saveType(type, leafResource))
                                .thenApply(alsoIgnored -> leafResource)
                        )
                )
        ).mapMaterializedValue(ignored -> NotUsed.notUsed());
    }

    // Add a leaf resource, (mark the leaf as complete,) and mark the leaf as a given serialisedFormat
    // Used for updating the persistence store from a given source of 'truth' - ie. a real resource-service
    @Override
    public <T extends LeafResource> Flow<T, T, NotUsed> withPersistenceBySerialisedFormat(final String serialisedFormat) {
        LOGGER.info("Persistence add for resources by serialisedFormat '{}'", serialisedFormat);
        // Persist that this serialisedFormat has (a potentially empty stream of) persisted info
        // Next time it is requested, it will be handled by persistence
        return Flow.lazyCompletionStageFlow(() -> isSerialisedFormatComplete(serialisedFormat)
                .thenCompose((Boolean formatAlreadyComplete) -> {
                    if (formatAlreadyComplete.equals(Boolean.FALSE)) {
                        return completenessRepository.futureSave(EntityType.FORMAT, serialisedFormat);
                    } else {
                        return CompletableFuture.completedFuture(null);
                    }
                })
                .thenApply(ignored -> Flow.<T>create()
                        .mapAsync(PARALLELISM, (T leafResource) -> saveResourceWithIncompleteParents(leafResource)
                                .thenCompose(alsoIgnored -> saveSerialisedFormat(serialisedFormat, leafResource))
                                .thenApply(alsoIgnored -> leafResource)
                        )
                )
        ).mapMaterializedValue(ignored -> NotUsed.notUsed());
    }
}
