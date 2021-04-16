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
import uk.gov.gchq.palisade.service.user.common.user.User;
import uk.gov.gchq.palisade.service.user.model.UserResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = {UserResponseTest.class})
class UserResponseTest {

    @Autowired
    private ObjectMapper mapper;

    /**
     * Tests the creation of the message type, UserResponse using the builder
     * plus tests the serialising to a Json string and deserialising to an object.
     *
     * @throws IOException throws if the {@link UserResponse} object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise or deserialise the Json string.
     */
    @Test
    void testUserResponseSerialisingAndDeserialising() throws IOException {
        var userResponse = UserResponse.Builder.create()
                .withUserId("originalUserId")
                .withResourceId("testResourceId")
                .withContext(new Context().purpose("testContext"))
                .withUser(new User().userId("testUserId"));

        var actualJson = mapper.writeValueAsString(userResponse);
        var actualInstance = mapper.readValue(actualJson, userResponse.getClass());

        assertThat(actualInstance)
                .as("Check using recursion, that the %s has been deserialized successfully", userResponse.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(userResponse);
    }
}