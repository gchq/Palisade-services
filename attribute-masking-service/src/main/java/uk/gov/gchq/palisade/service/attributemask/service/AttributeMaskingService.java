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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.io.IOException;

/**
 * The attribute-masking-service is the final transformation the palisade system applies
 * to resources before they are returned.
 * The service performs two functions:
 * - Store the full details of the authorised request in a persistence store, to be later
 * retrieved by the data-service
 * - Mask the leafResource, removing any sensitive information - this may later include
 * applying a separate set of attributeRules, distinct from resourceRules and recordRules
 */
public interface AttributeMaskingService {

    /**
     * Store the full details of the authorised request in a persistence store, to be later
     * retrieved by the data-service.
     *
     * @param token    the token {@link String} for the client request as a whole, created by the palisade-service
     * @param user     the {@link User} as authorised and returned by the user-service
     * @param resource one of many {@link LeafResource} as discovered and returned by the resource-service
     * @param context  the {@link Context} as originally supplied by the client
     * @param rules    the {@link Rules} that will be applied to the resource and its records as returned by the policy-service

     * @throws IOException if a failure occurred writing to the persistence store.
     */
    void storeAuthorisedRequest(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules) throws IOException;

    /**
     * Mask any sensitive attributes on a resource, possibly by applying attribute-level rules.
     *
     * @param resource the (sensitive) resource to be returned to the client
     * @return a copy of the resource with sensitive data masked or redacted
     */
    LeafResource maskResourceAttributes(final LeafResource resource);

}
