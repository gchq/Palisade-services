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

package uk.gov.gchq.palisade.contract.attributemask.h2;

import org.springframework.data.repository.CrudRepository;

import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;

import java.util.Optional;

/**
 * Mimic an external connection to the same database used by our service
 */
public interface AuthorisedRequestsRepositoryExternalConnection extends CrudRepository<AuthorisedRequestEntity, String> {

    Optional<AuthorisedRequestEntity> findByTokenAndResourceId(String token, String resourceId);

}
