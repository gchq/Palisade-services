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

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;

import java.io.IOException;

public class ReadException extends RuntimeException {
    private final DataReaderRequest readerRequest;

    /**
     * Specialised exception thrown by the data-service when an IOException occurred while reading
     * from the data-reader, bundling the data-reader request that caused the exception.
     *
     * @param cause a {@link IOException} that caused the error
     */
    public ReadException(final DataReaderRequest readerRequest, final IOException cause) {
        super("An exception was thrown while reading from data-reader", cause);
        this.readerRequest = readerRequest;
    }

    @Generated
    public DataReaderRequest getReaderRequest() {
        return readerRequest;
    }
}
