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

package uk.gov.gchq.palisade.component.data.service;

import uk.gov.gchq.palisade.data.serialise.Serialiser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public class TestSerialiser implements Serialiser<Object> {

    public TestSerialiser(final Class<?> domainClass) {
        // ignored, needed for construction only
    }

    @Override
    public InputStream serialise(final Stream<Object> objects) {
        return objects.filter(ReadChunkedDataServiceTest.TEST_RECORD::equals).findFirst()
                .map(testData -> (InputStream) new ByteArrayInputStream(testData.toString().getBytes()))
                .orElse(InputStream.nullInputStream());
    }

    @Override
    public Stream<Object> deserialise(final InputStream stream) {
        try {
            if (ReadChunkedDataServiceTest.TEST_RECORD.equals(new String(stream.readAllBytes()))) {
                return Stream.of(ReadChunkedDataServiceTest.TEST_RECORD);
            } else {
                return Stream.empty();
            }
        } catch (IOException e) {
            return Stream.empty();
        }
    }
}
