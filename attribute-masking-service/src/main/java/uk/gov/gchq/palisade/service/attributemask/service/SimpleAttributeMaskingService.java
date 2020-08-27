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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.repository.JpaPersistenceLayer;

public class SimpleAttributeMaskingService implements AttributeMaskingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAttributeMaskingService.class);

    private final JpaPersistenceLayer persistenceLayer;

    public SimpleAttributeMaskingService(final JpaPersistenceLayer persistenceLayer) {
        this.persistenceLayer = persistenceLayer;
    }

    @Override
    public boolean storeRequestResult(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
        try {
            this.persistenceLayer.put(token, user, resource, context, rules);
            return true;
        } catch (RuntimeException ex) {
            LOGGER.error("Caught error while writing to persistence store: ", ex);
            return false;
        }
    }

    @Override
    public LeafResource maskResourceAttributes(final LeafResource resource) {
        return resource;
    }
}
