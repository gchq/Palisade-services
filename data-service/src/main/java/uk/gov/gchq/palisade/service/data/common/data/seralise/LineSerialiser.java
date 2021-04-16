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
package uk.gov.gchq.palisade.service.data.common.data.seralise;

import uk.gov.gchq.palisade.service.data.serialise.SimpleStringSerialiser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * A Simple Seraliser used by the {@link SimpleStringSerialiser} to seralise and deseralise objects in the Hadoop reader package
 *
 * @param <T> the type of seraliser
 */
public abstract class LineSerialiser<T> implements Serialiser<T> {
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * Takes in a object and seralises it
     *
     * @param obj an object to be seralised
     * @return the seralised string value of the object passed in
     */
    public abstract String serialiseLine(final T obj);

    /**
     * Takes in a string and deseralises it back to the original object
     *
     * @param line the String to be deseralised
     * @return the deseralised string, back in its original object
     */
    public abstract T deserialiseLine(final String line);

    @Override
    public void serialise(final Stream<T> objects, final OutputStream output) {
        serialise(objects.iterator(), output);
    }

    /**
     * Using a PrintWriter, serialise each object in the iterator.
     *
     * @param itr    containing a stream of objects to be serialised
     * @param output an OutputStream used to send the serialised bytes to
     * @return the serialiser containing all the serialised strings
     */
    public Serialiser<T> serialise(final Iterator<T> itr, final OutputStream output) {
        requireNonNull(output, "output");
        requireNonNull(itr);
        try (PrintWriter printOut = new PrintWriter(new OutputStreamWriter(output, CHARSET))) {
            itr.forEachRemaining(item -> printOut.println(serialiseLine(item)));
        }
        return this;
    }

    @Override
    public Stream<T> deserialise(final InputStream stream) {
        if (isNull(stream)) {
            return Stream.empty();
        }
        return new BufferedReader(new InputStreamReader(stream, CHARSET))
                .lines()
                .map(this::deserialiseLine);
    }
}
