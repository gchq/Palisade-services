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

import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;

import static org.assertj.core.api.Assertions.assertThat;

class RulesConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RulesConverterTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RulesConverter rulesConverter = new RulesConverter(MAPPER);

    @Test
    void testRulesConverterIsConsistent() {
        // given we have a Rules object

        // when converted to a database column
        String databaseColumn = rulesConverter.convertToDatabaseColumn(ApplicationTestData.RULES);
        // then if converted to a column again, the result is identical
        assertThat(rulesConverter.convertToDatabaseColumn(ApplicationTestData.RULES)).isEqualTo(databaseColumn);

        // when the database column is converted back to a Rules object
        Rules<?> convertedRules = rulesConverter.convertToEntityAttribute(databaseColumn);
        // then if converted to a Rules object again, the result is identical
        assertThat(rulesConverter.convertToEntityAttribute(databaseColumn)).isEqualTo(convertedRules);
    }

    @Test
    void testRulesConverterIsCorrect() {
        // given we have a Rules object

        // when converted to and from a database
        String databaseColumn = rulesConverter.convertToDatabaseColumn(ApplicationTestData.RULES);
        Rules<?> convertedRules = rulesConverter.convertToEntityAttribute(databaseColumn);
        // then the returned Rules object is identical to the original
        assertThat(convertedRules).isEqualTo(ApplicationTestData.RULES);
        LOGGER.info("{} -> {}", ApplicationTestData.RULES, databaseColumn);
    }

    @Test
    void testRulesConverterHandlesNulls() {
        // given the Rules object being processed is null

        // when converted to and from a database
        String databaseColumn = rulesConverter.convertToDatabaseColumn(null);
        Rules<?> convertedRules = rulesConverter.convertToEntityAttribute(databaseColumn);

        // then no errors are thrown and the Rules object is still null
        assertThat(convertedRules).isNull();
    }

}
