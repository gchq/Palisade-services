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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataFlavourTest {

    String type = "type";
    String format = "format";

    @Test
    void testDatFlavour() {
        DataFlavour flavour = new DataFlavour(type, format);
        assertAll(
                () -> assertThat(flavour.getDataType())
                        .as("Check the expected type value")
                        .isEqualTo("type"),
                () -> assertThat(flavour.getSerialisedFormat())
                        .as("Check the expected format value")
                        .isEqualTo("format")
        );
    }

    @Test
    void testDataFlavourNoType() {
        assertThrows(IllegalArgumentException.class, () -> new DataFlavour("", format));
    }

    @Test
    void testDataFlavourNoFormat() {
        assertThrows(IllegalArgumentException.class, () -> new DataFlavour(type, ""));
    }
}
