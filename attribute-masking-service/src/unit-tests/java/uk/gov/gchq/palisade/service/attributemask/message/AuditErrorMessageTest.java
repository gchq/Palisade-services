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
package uk.gov.gchq.palisade.service.attributemask.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;

import static org.assertj.core.api.Assertions.assertThat;

class AuditErrorMessageTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void serialiseDeserialiseIsConsistent() throws JsonProcessingException {
        // Given some test data

        // When a Request is serialised and deserialised
        String serialisedRequest = MAPPER.writeValueAsString(ApplicationTestData.REQUEST);
        AttributeMaskingRequest deserialisedRequest = MAPPER.readValue(serialisedRequest, AttributeMaskingRequest.class);

        // Then the deserialised object is unchanged (equal)
        assertThat(deserialisedRequest).isEqualTo(ApplicationTestData.REQUEST);
    }
}
