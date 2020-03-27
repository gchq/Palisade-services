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

import org.apache.commons.io.IOUtils;

import uk.gov.gchq.palisade.Generated;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A read response that can be passed to clients. The {@link ClientReadResponse#asInputStream()} returns the given {@link InputStream}.
 * The {@link ClientReadResponse#writeTo(OutputStream)} method performs a straight copy to whatever {@link OutputStream} the client
 * provides. This method is unlikely to be needed by clients, but we have to provide it for symmetry.
 */
public class ClientReadResponse extends ReadResponse {

    /**
     * The stream the client can read from.
     */
    private final InputStream stream;

    /**
     * Create a response.
     *
     * @param stream the stream to provide to a client
     */
    public ClientReadResponse(final InputStream stream) {
        requireNonNull(stream, "stream");
        this.stream = stream;
    }

    @Override
    public InputStream asInputStream() {
        return stream;
    }

    @Override
    public void writeTo(final OutputStream output) throws IOException {
        requireNonNull(output, "output");
        //check this hasn't already been used
        boolean used = setUsed();
        if (used) {
            throw new IOException("writeTo can only be called once per instance");
        }

        IOUtils.copy(asInputStream(), output);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientReadResponse)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final ClientReadResponse that = (ClientReadResponse) o;
        return Objects.equals(stream, that.stream);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), stream);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ClientReadResponse.class.getSimpleName() + "[", "]")
                .add("stream=" + stream)
                .toString();
    }
}
