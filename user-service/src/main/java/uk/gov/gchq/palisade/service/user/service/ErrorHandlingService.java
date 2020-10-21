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

package uk.gov.gchq.palisade.service.user.service;

import uk.gov.gchq.palisade.service.user.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.user.model.UserRequest;

import java.util.Map;

/**
 * In the case that any error is thrown by the application and is not caught, this acts as the
 * final catch-all that will forward the details of failure to the appropriate location.
 * Provides a default creator for error messages, wrapping the {@link AuditErrorMessage} builder.
 * Care must be taken to avoid reporting errors from this service leading to a feedback loop.
 * If eg. the kafka error queue is unavailable, alternative actions must be taken instead.
 */
public interface ErrorHandlingService {

    /**
     * Report the error through the appropriate channels.
     *
     * @param token   the token for this request - used to notify the client of failures
     * @param request the request input that led to failure - the original request is extracted from this message
     */
    void reportError(final String token, final AuditErrorMessage request);

    /**
     * Helper method for mapping requests to errors.
     *
     * @param userRequest   original request input to the service
     * @param error         error thrown by the service at runtime
     * @param attributes    map of additional attributes to add to the error message
     * @return an error message containing the given details
     */
    default AuditErrorMessage createErrorMessage(final UserRequest userRequest, final Map<String, Object> attributes, final Throwable error) {
        return AuditErrorMessage.Builder.create(userRequest, attributes)
                .withError(error);
    }

}