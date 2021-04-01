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
package uk.gov.gchq.palisade.service.topicoffset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.topicoffset.common.Context;

import static org.assertj.core.api.Assertions.assertThat;

class ContextTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testContextShouldSerialiseAndDeseralise() throws JsonProcessingException {
        // When creating a new Context Object
        var context = new Context().purpose("purpose1");

        // And Then serialising and deseralising it
        var actualJson = mapper.writeValueAsString(context);
        var actualInstance = mapper.readValue(actualJson, Context.class);

        // Then
        assertThat(actualInstance)
                .as("Check using recursion, that the %s has been deserialized successfully", context.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(context);
    }
}