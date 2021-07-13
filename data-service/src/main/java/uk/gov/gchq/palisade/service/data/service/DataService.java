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

package uk.gov.gchq.palisade.service.data.service;

import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;

import java.util.concurrent.CompletableFuture;

public interface DataService {

    /**
     * Request the trusted details about a client's request from persistence (what policies to apply, user details, etc).
     *
     * @param request the client's request for a leaf resource and their unique request token
     * @return rules apply when accessing the data, returned as a {@link AuthorisedDataRequest} to pass to the
     * data-reader and null if there is no data
     * @throws ForbiddenException if there is no authorised data for the request
     */
    CompletableFuture<AuthorisedDataRequest> authoriseRequest(final DataRequest request);

}
