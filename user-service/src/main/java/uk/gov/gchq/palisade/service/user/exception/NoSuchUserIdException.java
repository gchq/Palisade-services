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
package uk.gov.gchq.palisade.service.user.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.user.common.UserId;
import uk.gov.gchq.palisade.service.user.service.UserService;

/**
 * A {@code NoSuchUserIdException} is a {@link RuntimeException} thrown by a
 * {@link UserService} implementation to
 * indicate that the requested {@link UserId} doesn't exist,
 * or is not known to that {@code Service} instance.
 */
public class NoSuchUserIdException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoSuchUserIdException.class);

    /**
     * Initializes this exception with no message or cause.
     */
    public NoSuchUserIdException() {
    }

    /**
     * Initializes this exception with the given message.
     *
     * @param message message for the exception
     */
    public NoSuchUserIdException(final String message) {
        super(message);
        LOGGER.info("NoSuchUserIdException thrown with '{}'", message);
    }

    /**
     * Initializes this exception with the given message and cause.
     *
     * @param message   message to report
     * @param throwable the underlying cause of this exception
     */
    public NoSuchUserIdException(final String message, final Throwable throwable) {
        super(message, throwable);
        LOGGER.error("NoSuchUserIdException thrown with '{}'", message);
    }
}
