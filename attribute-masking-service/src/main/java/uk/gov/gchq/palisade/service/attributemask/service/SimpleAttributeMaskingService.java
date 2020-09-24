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

package uk.gov.gchq.palisade.service.attributemask.service;

import org.springframework.lang.NonNull;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.repository.PersistenceLayer;

import java.util.concurrent.CompletableFuture;

/**
 * Simple implementation of the core of the attribute-masking-service.
 * Authorised requests are stored in the persistence layer, resources are not masked in any way.
 */
public class SimpleAttributeMaskingService implements AttributeMaskingService {
    private final PersistenceLayer persistenceLayer;
    private final LeafResourceMasker resourceMasker;

    /**
     * Constructor expected to be called by the ApplicationConfiguration, autowiring in the appropriate implementation of the repository (h2/redis/...)
     * as well as the appropriate masking function
     *
     * @param persistenceLayer the implementation of a PersistenceLayer to use
     * @param resourceMasker   the implementation of a LeafResourceMasker to use
     */
    public SimpleAttributeMaskingService(final PersistenceLayer persistenceLayer, final LeafResourceMasker resourceMasker) {
        this.persistenceLayer = persistenceLayer;
        this.resourceMasker = resourceMasker;
    }

    @Override
    public CompletableFuture<Void> storeAuthorisedRequest(final @NonNull String token, final @NonNull User user, final @NonNull LeafResource resource, final @NonNull Context context, final @NonNull Rules<?> rules) {
        return this.persistenceLayer.putAsync(token, user, resource, context, rules);
    }

    @Override
    public LeafResource maskResourceAttributes(final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
        return resourceMasker.apply(resource);
    }
}
