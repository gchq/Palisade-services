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
package uk.gov.gchq.palisade.component.user.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.model.UserRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = {UserRequestTest.class})
class UserRequestTest {

    @Autowired
    private ObjectMapper mapper;

    /**
     * Tests the creation of the message type, UserRequest using the builder
     * plus tests the serializing to a Json string and deserializing to an object.
     *
     * @throws IOException throws if the {@link UserRequest} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialize or deserialize the Json string.
     */
    @Test
    void testUserRequestSerialisingAndDeserialising() throws IOException {
        var userRequest = UserRequest.Builder.create()
                .withUserId("testUserId")
                .withResourceId("testResourceId")
                .withContext(new Context().purpose("testContext"));

        var actualJson = mapper.writeValueAsString(userRequest);
        var actualInstance = mapper.readValue(actualJson, userRequest.getClass());

        assertThat(actualInstance)
                .as("Check using recursion, that the %s has been deserialized successfully", userRequest.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(userRequest);
    }
}