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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.service.data.exception.ReadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * An {@code AvroInputStreamSerialiser} is used to serialise and deserialise Avro files.
 * Converts an avro {@link InputStream} to/from a {@link Stream} of domain objects ({@link O}s).
 *
 * @param <O> the domain object type
 */
public class AvroSerialiser<O> implements Serialiser<O> {
    private static final int PARALLELISM = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(AvroSerialiser.class);
    private static final ThreadPoolTaskExecutor EXECUTOR = new ThreadPoolTaskExecutor();

    static {
        EXECUTOR.setCorePoolSize(PARALLELISM);
    }

    private final ReflectDatumWriter<O> datumWriter;
    private final Schema schema;

    /**
     * Constructor for the {@link AvroSerialiser}
     *
     * @param domainClass the class for the serialiser
     */
    @JsonCreator
    public AvroSerialiser(@JsonProperty("domainClass") final Class<O> domainClass) {
        requireNonNull(domainClass, "domainClass is required");
        this.schema = ReflectData.AllowNull.get().getSchema(domainClass);
        this.datumWriter = new ReflectDatumWriter<>(schema);
    }

    @Override
    public Stream<O> deserialise(final InputStream input) {
        DataFileStream<O> in;
        try {
            in = new DataFileStream<>(input, new ReflectDatumReader<>(schema));
        } catch (IOException e) {
            throw new ReadException("An error occurred during deserialisaton", e);
        }

        // Don't use try-with-resources here! This input stream needs to stay open until it is closed manually by the
        // stream it is feeding below
        return StreamSupport.stream(in.spliterator(), false);
    }

    @Override
    public InputStream serialise(final Stream<O> objects) {
        PipedInputStream is = new PipedInputStream();
        Runnable pipeWriter = () -> {
            try (PipedOutputStream os = new PipedOutputStream(); DataFileWriter<O> dataFileWriter = new DataFileWriter<>(datumWriter)) {
                os.connect(is);
                if (nonNull(objects)) {
                    // create a data file writer around the output stream
                    LOGGER.debug("Creating data file writer");
                    dataFileWriter.create(schema, os);
                    Iterator<O> objectIt = objects.iterator();
                    while (objectIt.hasNext()) {
                        O next = objectIt.next();
                        dataFileWriter.append(next);
                    }
                }
            } catch (IOException e) {
                throw new ReadException("An error occurred during serialisation", e);
            }
        };
        EXECUTOR.execute(pipeWriter);
        return is;
    }
}
