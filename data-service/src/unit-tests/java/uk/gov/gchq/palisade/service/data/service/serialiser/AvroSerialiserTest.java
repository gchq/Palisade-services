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

package uk.gov.gchq.palisade.service.data.service.serialiser;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.Generated;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AvroSerialiserTest {
    static class Record {
        private String field;

        @SuppressWarnings("unused")
        Record() {
            // Used for serialisation
        }

        Record(final String field) {
            this.field = field;
        }

        @Generated
        public String getField() {
            return field;
        }

        @Generated
        public void setField(final String field) {
            this.field = Optional.ofNullable(field)
                    .orElseThrow(() -> new IllegalArgumentException("field cannot be null"));
        }

        @Override
        @Generated
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Record)) {
                return false;
            }
            final Record record = (Record) o;
            return Objects.equals(field, record.field);
        }

        @Override
        @Generated
        public int hashCode() {
            return Objects.hash(field);
        }
    }

    AvroSerialiser<Record> serialiser = new AvroSerialiser<>(Record.class);

    @Test
    void testSerialiseAndDeserialise() {
        // Given
        var records = List.of(new Record("record one"), new Record("record two"));

        // When
        var inputStream = serialiser.serialise(records.stream());
        var recordStream = serialiser.deserialise(inputStream);

        // Then
        assertThat(recordStream.collect(Collectors.toList()))
                .isEqualTo(records);
    }
}