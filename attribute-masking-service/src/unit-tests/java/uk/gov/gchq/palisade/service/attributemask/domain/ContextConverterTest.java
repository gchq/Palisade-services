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
import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;

import static org.assertj.core.api.Assertions.assertThat;

class ContextConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextConverterTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ContextConverter contextConverter = new ContextConverter(MAPPER);

    @Test
    void testContextConverterIsConsistent() {
        // given we have a Context object

        // when converted to a database column
        String databaseColumn = contextConverter.convertToDatabaseColumn(ApplicationTestData.CONTEXT);
        // then if converted to a column again, the result is identical
        assertThat(contextConverter.convertToDatabaseColumn(ApplicationTestData.CONTEXT)).isEqualTo(databaseColumn);

        // when the database column is converted back to a Context object
        Context convertedContext = contextConverter.convertToEntityAttribute(databaseColumn);
        // then if converted to a Context object again, the result is identical
        assertThat(contextConverter.convertToEntityAttribute(databaseColumn)).isEqualTo(convertedContext);
    }

    @Test
    void testContextConverterIsCorrect() {
        // given we have a Context object

        // when converted to and from a database
        String databaseColumn = contextConverter.convertToDatabaseColumn(ApplicationTestData.CONTEXT);
        Context convertedContext = contextConverter.convertToEntityAttribute(databaseColumn);
        // then the returned Context object is identical to the original
        assertThat(convertedContext).isEqualTo(ApplicationTestData.CONTEXT);
        LOGGER.info("{} -> {}", ApplicationTestData.CONTEXT, databaseColumn);
    }

    @Test
    void testContextConverterHandlesNulls() {
        // given the Context object being processed is null

        // when converted to and from a database
        String databaseColumn = contextConverter.convertToDatabaseColumn(null);
        Context convertedContext = contextConverter.convertToEntityAttribute(databaseColumn);

        // then no errors are thrown and the Context object is still null
        assertThat(convertedContext).isNull();
    }

}
