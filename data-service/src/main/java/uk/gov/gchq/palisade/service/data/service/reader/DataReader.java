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

package uk.gov.gchq.palisade.service.data.service.reader;

import akka.Done;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;

import uk.gov.gchq.palisade.resource.LeafResource;

import java.io.InputStream;
import java.util.concurrent.CompletionStage;

/**
 * Part of the core set of interfaces for the Data Service.
 * The DataReader is responsible for reading the raw bytes of a {@link LeafResource} from the appropriate data-source.
 * This may be a local file, a remote stream or any other mapping of resources to bytes.
 */
public interface DataReader {

    /**
     * Can this reader attempt to produce bytes for this resource.
     * This is likely based off the resource URI scheme and perhaps the authority.
     * If this returns false, it is a guarantee that {@link DataReader#read(LeafResource)} will throw an exception.
     * If this returns true, {@link DataReader#read(LeafResource)} may still throw e.g. if the resource does not exist
     * or was otherwise inaccessible.
     *
     * @param leafResource the resource to check if acceptable by this reader and therefore hopefully readable
     * @return true if this reader should attempt to read it, false otherwise
     */
    boolean accepts(final LeafResource leafResource);

    /**
     * Read a leafResource, returning the stream of bytes it contained.
     * Deserialisation and rule-application does not happen here.
     *
     * @param leafResource the resource to read
     * @return a stream of bytes
     */
    InputStream read(final LeafResource leafResource);

    /**
     * Default wrapper around Java-stdlib {@link DataReader#read(LeafResource)} that may be overridden to optimise
     * for the given storage technology if appropriate.
     *
     * @param leafResource the resource to read
     * @return an akka {@link Source} of {@link ByteString}s equivalent to the aforementioned implemented {@link InputStream}
     */
    default Source<ByteString, CompletionStage<Done>> readSource(final LeafResource leafResource) {
        return StreamConverters.fromInputStream(() -> read(leafResource))
                .mapMaterializedValue(future -> future.thenApply(io -> Done.done()));
    }
}
