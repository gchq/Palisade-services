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

package uk.gov.gchq.palisade.service.data.common.data.reader;

import uk.gov.gchq.palisade.service.data.common.Generated;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * This class is used to pass back to the data service the stream of data in the
 * format expected by the client, along with any error/info messages for the client.
 */
public class DataReaderResponse {
    private ResponseWriter writer;
    private String message;

    public DataReaderResponse() {
        // no args constructor required
    }

    /**
     * Set the {@link ResponseWriter} object for this {@link DataReaderResponse} object.
     *
     * @param writer the data writer object
     * @return the {@link DataReaderResponse}
     */
    @Generated
    public DataReaderResponse writer(final ResponseWriter writer) {
        requireNonNull(writer, "The writer cannot be set to null.");
        this.setWriter(writer);
        return this;
    }

    /**
     * Set the message value for this {@link DataReaderResponse} object.
     *
     * @param message an error/info message to be returned to the client
     * @return the {@link DataReaderResponse}
     */
    @Generated
    public DataReaderResponse message(final String message) {
        requireNonNull(message, "The message cannot be set to null.");
        this.setMessage(message);
        return this;
    }

    @Generated
    public ResponseWriter getWriter() {
        return writer;
    }

    @Generated
    public void setWriter(final ResponseWriter writer) {
        requireNonNull(writer);
        this.writer = writer;
    }

    @Generated
    public String getMessage() {
        return message;
    }

    @Generated
    public void setMessage(final String message) {
        requireNonNull(message);
        this.message = message;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataReaderResponse)) {
            return false;
        }
        final DataReaderResponse that = (DataReaderResponse) o;
        return Objects.equals(writer, that.writer) &&
                Objects.equals(message, that.message);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(writer, message);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", DataReaderResponse.class.getSimpleName() + "[", "]")
                .add("writer=" + writer)
                .add("message='" + message + "'")
                .toString();
    }
}
