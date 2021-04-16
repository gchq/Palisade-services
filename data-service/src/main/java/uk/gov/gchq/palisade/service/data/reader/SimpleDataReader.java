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

package uk.gov.gchq.palisade.service.data.reader;

import uk.gov.gchq.palisade.service.data.common.data.reader.SerialisedDataReader;
import uk.gov.gchq.palisade.service.data.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.exception.ReadException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;

/**
 * A simple extension of the {@link SerialisedDataReader} to allow the Data Service to read from the local file system.
 */
public class SimpleDataReader extends SerialisedDataReader {

    @Override
    protected InputStream readRaw(final LeafResource resource) {
        try {
            return new FileInputStream(Paths.get(URI.create(resource.getId())).toFile());
        } catch (FileNotFoundException e) {
            throw new ReadException("Could not find file", e);
        }
    }
}
