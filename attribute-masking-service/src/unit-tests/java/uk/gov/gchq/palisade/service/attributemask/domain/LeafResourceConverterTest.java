/*
 * Copyright 2020 Crown Copyright
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

package uk.gov.gchq.palisade.service.attributemask.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;

import static org.assertj.core.api.Assertions.assertThat;

class LeafResourceConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeafResourceConverterTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final LeafResourceConverter leafResourceConverter = new LeafResourceConverter(MAPPER);

    @Test
    void testLeafResourceConverterIsConsistent() {
        // given we have a LeafResource object

        // when converted to a database column
        String databaseColumn = leafResourceConverter.convertToDatabaseColumn(ApplicationTestData.LEAF_RESOURCE);
        // then if converted to a column again, the result is identical
        assertThat(leafResourceConverter.convertToDatabaseColumn(ApplicationTestData.LEAF_RESOURCE)).isEqualTo(databaseColumn);

        // when the database column is converted back to a LeafResource object
        LeafResource convertedLeafResource = leafResourceConverter.convertToEntityAttribute(databaseColumn);
        // then if converted to a LeafResource object again, the result is identical
        assertThat(leafResourceConverter.convertToEntityAttribute(databaseColumn)).isEqualTo(convertedLeafResource);
    }

    @Test
    void testLeafResourceConverterIsCorrect() {
        // given we have a LeafResource object

        // when converted to and from a database
        String databaseColumn = leafResourceConverter.convertToDatabaseColumn(ApplicationTestData.LEAF_RESOURCE);
        LeafResource convertedLeafResource = leafResourceConverter.convertToEntityAttribute(databaseColumn);
        // then the returned LeafResource object is identical to the original
        assertThat(convertedLeafResource).isEqualTo(ApplicationTestData.LEAF_RESOURCE);
        LOGGER.info("{} -> {}", ApplicationTestData.LEAF_RESOURCE, databaseColumn);
    }

    @Test
    void testLeafResourceConverterHandlesNulls() {
        // given the LeafResource object being processed is null

        // when converted to and from a database
        String databaseColumn = leafResourceConverter.convertToDatabaseColumn(null);
        LeafResource convertedLeafResource = leafResourceConverter.convertToEntityAttribute(databaseColumn);

        // then no errors are thrown and the LeafResource object is still null
        assertThat(convertedLeafResource).isNull();
    }

}
