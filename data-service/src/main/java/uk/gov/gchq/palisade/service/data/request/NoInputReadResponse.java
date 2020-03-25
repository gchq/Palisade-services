/*
 * Copyright 2018 Crown Copyright
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

package uk.gov.gchq.palisade.service.data.request;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.common.ResponseWriter;
import uk.gov.gchq.palisade.reader.request.DataReaderResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A reader response that is intended to be used server side to provide a writer that will be used to write data
 * back to the client. For this reason, attempting to obtain an {@link InputStream} will throw an exception.
 */
public class NoInputReadResponse extends ReadResponse {

    /**
     * The {@link DataReader} response that a writer can be sourced from.
     */
    private final DataReaderResponse readerResponse;

    /**
     * Create an instance.
     *
     * @param readerResponse the response from a {@link DataReader}.
     */
    public NoInputReadResponse(final DataReaderResponse readerResponse) {
        requireNonNull(readerResponse, "readerResponse");
        this.readerResponse = readerResponse;
    }

    /**
     * Always throws an {@link IOException}.
     *
     * @return this method never returns
     * @throws IOException when invoked
     */
    @Override
    public InputStream asInputStream() throws IOException {
        throw new IOException("No InputStream is available. Please use writeTo()!");
    }

    @Override
    public void writeTo(final OutputStream output) throws IOException {
        requireNonNull(output, "output");
        //check this hasn't already been used
        boolean used = setUsed();
        if (used) {
            throw new IOException("writeTo can only be called once per instance");
        }

        //write all data to the given stream and ensure stream closed
        try (ResponseWriter writer = readerResponse.getWriter()) {
            writer.write(output);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NoInputReadResponse)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final NoInputReadResponse that = (NoInputReadResponse) o;
        return Objects.equals(readerResponse, that.readerResponse);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), readerResponse);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", NoInputReadResponse.class.getSimpleName() + "[", "]")
                .add("readerResponse=" + readerResponse)
                .toString();
    }
}
