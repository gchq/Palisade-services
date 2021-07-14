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

package uk.gov.gchq.palisade.service.data.service.serialiser;

import uk.gov.gchq.palisade.data.serialise.Serialiser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Stream;

/**
 * Default serialiser for text/plain files, a do-nothing serialiser that maps
 * a multiline {@link InputStream} into a {@link Stream} of lines.
 */
public class StringLineSerialiser implements Serialiser<String> {
    /**
     * Dummy constructor, the domain must be {@link String} and no further operations are required
     *
     * @param domain the Java stdlib {@link String} {@link Class}
     */
    public StringLineSerialiser(final Class<String> domain) {
        // Empty constructor for initialisation only
    }

    /**
     * @inheritDoc
     */
    @Override
    public InputStream serialise(final Stream<String> objects) {
        var bytes = objects.reduce((l, r) -> l + "\n" + r)
                .orElse("")
                .getBytes(Charset.defaultCharset());
        return new ByteArrayInputStream(bytes);
    }

    /**
     * @inheritDoc
     */
    @Override
    public Stream<String> deserialise(final InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()))
                .lines();
    }
}
