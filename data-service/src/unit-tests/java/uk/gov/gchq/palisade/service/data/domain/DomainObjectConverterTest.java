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

package uk.gov.gchq.palisade.service.data.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import javax.persistence.AttributeConverter;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DomainObjectConverterTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static class DomainConvertersSource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(new ContextConverter(MAPPER), DomainTestData.CONTEXT),
                    Arguments.of(new LeafResourceConverter(MAPPER), DomainTestData.LEAF_RESOURCE),
                    Arguments.of(new RulesConverter(MAPPER), DomainTestData.RULES),
                    Arguments.of(new UserConverter(MAPPER), DomainTestData.USER)
            );
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DomainConvertersSource.class)
    <T> void testConverterIsConsistent(final AttributeConverter<T, String> converter, final T object) {
        // given we have an object

        // when converted to a database column
        String databaseColumn = converter.convertToDatabaseColumn(object);
        // then if converted to a column again, the result is identical
        assertThat(converter.convertToDatabaseColumn(object))
                .as("")
                .isEqualTo(databaseColumn);

        // when the database column is converted back to a Context object
        T convertedObject = converter.convertToEntityAttribute(databaseColumn);
        // then if converted to a Context object again, the result is identical
        assertThat(converter.convertToEntityAttribute(databaseColumn))
                .isEqualTo(convertedObject);
    }

    @ParameterizedTest
    @ArgumentsSource(DomainConvertersSource.class)
    <T> void testConverterIsCorrect(final AttributeConverter<T, String> converter, final T object) {
        // given we have an object

        // when converted to and from a database column
        String databaseColumn = converter.convertToDatabaseColumn(object);
        T convertedObject = converter.convertToEntityAttribute(databaseColumn);

        // then the returned Context object is identical to the original
        assertThat(convertedObject)
                .isEqualTo(object);
    }

    @ParameterizedTest
    @ArgumentsSource(DomainConvertersSource.class)
    <T> void testConverterHandlesNulls(final AttributeConverter<T, String> converter, final T ignored) {
        // given the Context object being processed is null

        // when converted to and from a database
        String databaseColumn = converter.convertToDatabaseColumn(null);
        T convertedObject = converter.convertToEntityAttribute(databaseColumn);

        // then no errors are thrown and the Context object is still null
        assertThat(convertedObject)
                .isNull();
    }

}
