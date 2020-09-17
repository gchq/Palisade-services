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

package uk.gov.gchq.palisade.service.attributemask.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse.Builder;
import uk.gov.gchq.palisade.service.attributemask.message.AttributeMaskingResponse.Builder.IResource;
import uk.gov.gchq.palisade.service.attributemask.message.StreamMarker;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class MarkedStreamController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarkedStreamController.class);

    private final AttributeMaskingService attributeMaskingService;

    public MarkedStreamController(final AttributeMaskingService attributeMaskingService) {
        this.attributeMaskingService = attributeMaskingService;
    }

    /**
     * Common service interface independent of technology (REST / Kafka)
     * At least one of the streamMarker or the request must be non-null, or throws a NullPointerException.
     * If both are non-null, precedence is given to the stream-marker
     *
     * @param token                the token for the client's request, stored in headers
     * @param nullableStreamMarker the (optional) start/end of stream marker for the client's request, stored in headers
     * @param nullableRequest      the (optional) request itself
     * @return the response from the service
     * @throws IOException if an error occurred (as well as RuntimeExceptions)
     */
    protected Optional<AttributeMaskingResponse> processRequestOrStreamMarker(
            final String token,
            final @Nullable StreamMarker nullableStreamMarker,
            final @Nullable AttributeMaskingRequest nullableRequest) throws IOException {
        // Only process if no stream marker
        if (nullableStreamMarker == null) {
            AttributeMaskingRequest request = requireNonNull(nullableRequest, "Either streamMarker or request must be non-null");
            IResource responseBuilder = Builder.create(request);
            LeafResource maskedResource = processRequest(token, request);
            return Optional.of(maskedResource)
                    .map(responseBuilder::withResource);

        } else {
            processStreamMarker(token, nullableStreamMarker);
            return Optional.empty();
        }
    }

    private LeafResource processRequest(final String token, final AttributeMaskingRequest request) throws IOException {
        // Store authorised request with service
        LOGGER.info("Storing request result for token {} and resourceId {}", token, request.getResource().getId());
        LOGGER.debug("Request is {}", request);
        // Throw an exception if the request was not stored successfully
        attributeMaskingService.storeAuthorisedRequest(token,
                request.getUser(),
                request.getResource(),
                request.getContext(),
                request.getRules()
        );
        // Return the masked resource
        return attributeMaskingService.maskResourceAttributes(request.getResource());
    }

    private void processStreamMarker(final String token, final StreamMarker streamMarker) {
        // Nothing to do if this is a stream marker, transparently pass on the message
        LOGGER.debug("Observed stream-marker {} for token {}", streamMarker, token);
    }
}
