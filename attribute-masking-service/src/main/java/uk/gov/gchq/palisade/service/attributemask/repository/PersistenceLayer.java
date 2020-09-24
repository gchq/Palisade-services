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
package uk.gov.gchq.palisade.service.attributemask.repository;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.concurrent.CompletableFuture;

public interface PersistenceLayer {

    /**
     * Persist an authorised request. This is a write-only interface, while the data-service holds a read-only interface.
     *
     * @param token    the token {@link String} for the client request as a whole, created by the palisade-service
     * @param user     the {@link User} as authorised and returned by the user-service
     * @param resource one of many {@link LeafResource} as discovered and returned by the resource-service
     * @param context  the {@link Context} as originally supplied by the client
     * @param rules    the {@link Rules} that will be applied to the resource and its records as returned by the policy-service
     */
    CompletableFuture<Void> putAsync(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules);

}
