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

package uk.gov.gchq.palisade.service.data.exception;

import uk.gov.gchq.palisade.service.data.service.DataService;

/**
 * {@link RuntimeException} to be thrown by a {@link DataService} to indicate a failure while processing a request.
 * This failure should be caught and audited with the audit service.
 */
public class ReadException extends RuntimeException {

    /**
     * Instantiates a new Read exception with a {@link Throwable} cause
     * which will call super and throw a {@link RuntimeException}
     *
     * @param cause the reason for throwing the exception
     */
    public ReadException(final Throwable cause) {
        super(cause);
    }
}
