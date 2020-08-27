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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;

import javax.transaction.Transactional;

import static java.util.Objects.requireNonNull;

public class JpaPersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaPersistenceLayer.class);

    private final AuthorisedRequestsRepository authorisedRequestsRepository;

    public JpaPersistenceLayer(final AuthorisedRequestsRepository authorisedRequestsRepository) {
        this.authorisedRequestsRepository = requireNonNull(authorisedRequestsRepository, "AuthorisedRequestsRepository cannot be null");
    }

    @Override
    @Transactional
    public void put(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
        this.authorisedRequestsRepository.save(token, user, resource, context, rules);

        LOGGER.debug("Persisted authorised request for unique pair {}-{}", token, resource.getId());
    }
}
