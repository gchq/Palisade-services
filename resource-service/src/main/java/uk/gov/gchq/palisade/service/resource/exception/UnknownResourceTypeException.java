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

package uk.gov.gchq.palisade.service.resource.exception;


import uk.gov.gchq.palisade.reader.common.resource.Resource;

/**
 * An {@code UnknownResourceType} is a {@link RuntimeException} thrown by the
 * Resource Service to indicate that a {@link Resource}
 * is neither a parent or a leaf resource.
 */
public class UnknownResourceTypeException extends RuntimeException {

    /**
     * Initialises this exception with the given message.
     *
     * @param message message for the exception
     */
    public UnknownResourceTypeException(final String message) {
        super(message);
    }

    /**
     * Initialises this exception with the given message and cause.
     *
     * @param message   message to report
     * @param throwable the underlying cause of this exception
     */
    public UnknownResourceTypeException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
