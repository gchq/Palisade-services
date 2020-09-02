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
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplicationTestData;

import static org.assertj.core.api.Assertions.assertThat;

class LeafResourceConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeafResourceConverterTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final LeafResourceConverter leafResourceConverter = new LeafResourceConverter(MAPPER);

    @Test
    void testLeafResourceConverterIsConsistent() {
        // given - leafResource

        // when
        String databaseColumn = leafResourceConverter.convertToDatabaseColumn(AttributeMaskingApplicationTestData.LEAF_RESOURCE);
        // then
        assertThat(leafResourceConverter.convertToDatabaseColumn(AttributeMaskingApplicationTestData.LEAF_RESOURCE)).isEqualTo(databaseColumn);

        // when
        LeafResource convertedLeafResource = leafResourceConverter.convertToEntityAttribute(databaseColumn);
        // then
        assertThat(leafResourceConverter.convertToEntityAttribute(databaseColumn)).isEqualTo(convertedLeafResource);
    }

    @Test
    void testLeafResourceConverterIsCorrect() {
        // given - leafResource

        // when
        String databaseColumn = leafResourceConverter.convertToDatabaseColumn(AttributeMaskingApplicationTestData.LEAF_RESOURCE);
        LeafResource convertedLeafResource = leafResourceConverter.convertToEntityAttribute(databaseColumn);
        // then
        assertThat(convertedLeafResource).isEqualTo(AttributeMaskingApplicationTestData.LEAF_RESOURCE);
        LOGGER.info("{} -> {}", AttributeMaskingApplicationTestData.LEAF_RESOURCE, databaseColumn);
    }

    @Test
    void testLeafResourceConverterHandlesNulls() {
        // given - nothing

        // when
        String databaseColumn = leafResourceConverter.convertToDatabaseColumn(null);
        LeafResource convertedLeafResource = leafResourceConverter.convertToEntityAttribute(databaseColumn);

        // then
        assertThat(convertedLeafResource).isNull();
    }

}
