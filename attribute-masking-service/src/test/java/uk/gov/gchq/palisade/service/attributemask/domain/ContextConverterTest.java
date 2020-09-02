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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplicationTestData;

import static org.assertj.core.api.Assertions.assertThat;

class ContextConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextConverterTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ContextConverter contextConverter = new ContextConverter(MAPPER);

    @Test
    void testContextConverterIsConsistent() {
        // given - context

        // when
        String databaseColumn = contextConverter.convertToDatabaseColumn(AttributeMaskingApplicationTestData.CONTEXT);
        // then
        assertThat(contextConverter.convertToDatabaseColumn(AttributeMaskingApplicationTestData.CONTEXT)).isEqualTo(databaseColumn);

        // when
        Context convertedContext = contextConverter.convertToEntityAttribute(databaseColumn);
        // then
        assertThat(contextConverter.convertToEntityAttribute(databaseColumn)).isEqualTo(convertedContext);
    }

    @Test
    void testContextConverterIsCorrect() {
        // given - context

        // when
        String databaseColumn = contextConverter.convertToDatabaseColumn(AttributeMaskingApplicationTestData.CONTEXT);
        Context convertedContext = contextConverter.convertToEntityAttribute(databaseColumn);
        // then
        assertThat(convertedContext).isEqualTo(AttributeMaskingApplicationTestData.CONTEXT);
        LOGGER.info("{} -> {}", AttributeMaskingApplicationTestData.CONTEXT, databaseColumn);
    }

    @Test
    void testContextConverterHandlesNulls() {
        // given - nothing

        // when
        String databaseColumn = contextConverter.convertToDatabaseColumn(null);
        Context convertedContext = contextConverter.convertToEntityAttribute(databaseColumn);

        // then
        assertThat(convertedContext).isNull();
    }

}
