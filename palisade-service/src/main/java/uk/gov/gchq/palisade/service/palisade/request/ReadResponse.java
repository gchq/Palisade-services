/*
 * Copyright 2019 Crown Copyright
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


package uk.gov.gchq.palisade.service.palisade.request;

import uk.gov.gchq.palisade.Generated;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public abstract class ReadResponse {
    private String message;
    /**
     * Specifies if the data stream has been retrieved from this response.
     */
    private AtomicBoolean isUsed = new AtomicBoolean(false);

    /**
     * Retrieves the data returned from the request as an {@link InputStream}. This method can only be called once.
     *
     * @return a stream of data from Palisade
     * @throws IOException if {@link ReadResponse#isUsed} returns {@code true}, or an underlying IO error occurs
     */
    public abstract InputStream asInputStream() throws IOException;

    /**
     * Instructs the data stream from Palisade be copied to the given {@link OutputStream}. This method can only be called
     * once.
     *
     * @param output the stream to copy to the data to
     * @throws IOException if {@link ReadResponse#isUsed} returns {@code true}, or an underlying IO error occurs
     */
    public abstract void writeTo(final OutputStream output) throws IOException;

    /**
     * Tests whether the data stream from this instance has already been retrieved, either as an {@link InputStream} or
     * copied to another stream.
     *
     * @return true if the stream has already been used
     * @see ReadResponse#asInputStream()
     * @see ReadResponse#writeTo(OutputStream)
     */
    @Generated
    public boolean isUsed() {
        return isUsed.get();
    }

    /**
     * Sets the data stream as retrieved (atomically).
     *
     * @return the previous value
     */
    @Generated
    protected boolean setUsed() {
        return isUsed.getAndSet(true);
    }

    @Generated
    public ReadResponse message(final String message) {
        requireNonNull(message, "The message cannot be set to null.");
        this.setMessage(message);
        return this;
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
        if (!(o instanceof ReadResponse)) {
            return false;
        }
        final ReadResponse that = (ReadResponse) o;
        return Objects.equals(message, that.message) &&
                Objects.equals(isUsed, that.isUsed);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(message, isUsed);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ReadResponse.class.getSimpleName() + "[", "]")
                .add("message='" + message + "'")
                .add("isUsed=" + isUsed)
                .toString();
    }
}
