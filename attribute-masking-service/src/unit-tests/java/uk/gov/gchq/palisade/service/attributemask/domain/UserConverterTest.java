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

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;

import static org.assertj.core.api.Assertions.assertThat;

class UserConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserConverterTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final UserConverter userConverter = new UserConverter(MAPPER);

    @Test
    void testUserConverterIsConsistent() {
        // given - user

        // when
        String databaseColumn = userConverter.convertToDatabaseColumn(ApplicationTestData.USER);
        // then
        assertThat(userConverter.convertToDatabaseColumn(ApplicationTestData.USER)).isEqualTo(databaseColumn);

        // when
        User convertedUser = userConverter.convertToEntityAttribute(databaseColumn);
        // then
        assertThat(userConverter.convertToEntityAttribute(databaseColumn)).isEqualTo(convertedUser);
    }

    @Test
    void testUserConverterIsCorrect() {
        // given - user

        // when
        String databaseColumn = userConverter.convertToDatabaseColumn(ApplicationTestData.USER);
        User convertedUser = userConverter.convertToEntityAttribute(databaseColumn);
        // then
        assertThat(convertedUser).isEqualTo(ApplicationTestData.USER);
        LOGGER.info("{} -> {}", ApplicationTestData.USER, databaseColumn);
    }

    @Test
    void testUserConverterHandlesNulls() {
        // given - nothing

        // when
        String databaseColumn = userConverter.convertToDatabaseColumn(null);
        User convertedUser = userConverter.convertToEntityAttribute(databaseColumn);

        // then
        assertThat(convertedUser).isNull();
    }

}
