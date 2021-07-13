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

public interface DataReader {

    boolean accepts(final LeafResource leafResource);

    InputStream read(final LeafResource leafResource);

    default Source<ByteString, CompletionStage<Done>> readSource(final LeafResource leafResource) {
        return StreamConverters.fromInputStream(() -> read(leafResource))
                .mapMaterializedValue(future -> future.thenApply(io -> Done.done()));
    }
}
