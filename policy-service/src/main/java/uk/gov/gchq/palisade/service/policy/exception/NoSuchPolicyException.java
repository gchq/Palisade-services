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
package uk.gov.gchq.palisade.service.policy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@code NoSuchPolicyException} is a {@link RuntimeException} thrown by a
 * {@link uk.gov.gchq.palisade.service.policy.service.PolicyService} implementation to
 * indicate that the requested {@link uk.gov.gchq.palisade.rule.Rule} doesnt exist for the requested {@link uk.gov.gchq.palisade.resource.Resource} doesn't exist,
 * or is not known to that {@code Service} instance.
 */
public class NoSuchPolicyException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoSuchPolicyException.class);


    /**
     * Initialises this exception with no message or cause.
     */
    public NoSuchPolicyException() {
    }

    /**
     * Initialises this exception with the given message.
     *
     * @param message message for the exception
     */
    public NoSuchPolicyException(final String message) {
        super(message);
        LOGGER.info("NoSuchPolicyException thrown with {} message", message);
    }

    /**
     * Initialises this exception with the given message and cause.
     *
     * @param message   message to report
     * @param throwable the underlying cause of this exception
     */
    public NoSuchPolicyException(final String message, final Throwable throwable) {
        super(message, throwable);
        LOGGER.error("NoSuchPolicyException thrown with {} message", message);
    }
}
