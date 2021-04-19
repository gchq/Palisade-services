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
package uk.gov.gchq.palisade.service.data.exception;

import java.io.IOException;

/**
 * An exception thrown when a request is made to a data-service via a ReadRequest
 * or to a {@link uk.gov.gchq.palisade.service.data.reader.DataReader} via a {@link uk.gov.gchq.palisade.service.data.model.DataReaderRequest}
 * for a resource that cannot currently be served due to a lack of capacity to serve the request.
 */
public class NoCapacityException extends IOException {

    /**
     * Constructs a new {@link NoCapacityException} with the specified detail message.
     *
     * @param message a {@link String} value detailing the error
     */
    public NoCapacityException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@link NoCapacityException} with the cause.
     *
     * @param cause a {@link Throwable} that caused the error
     */
    public NoCapacityException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@link NoCapacityException} with the specified detail message and cause.
     *
     * @param message a {@link String} value detailing the error
     * @param cause   a {@link Throwable} that caused the error
     */
    public NoCapacityException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
