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

/**
 * Exception thrown when a named serialiser cannot be initialised, e.g. because it doesn't accept the domain class.
 */
public class SerialiserInitialisationException extends RuntimeException {
    /**
     * Constructs a new {@link SerialiserInitialisationException} with the specified detail message and cause.
     *
     * @param message a {@link String} value detailing the error
     */
    public SerialiserInitialisationException(final String message) {
        super(message);
    }
}
