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

package uk.gov.gchq.palisade.service.policy.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class PolicyRequestTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(uk.gov.gchq.palisade.service.policy.message.PolicyResponseTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Context context = new Context().purpose("test-purpose");
    private static final String token = "test-token";
    private static final User user = new User().userId("test-userId");
    private static final LeafResource resource = new FileResource().id("/test/file.format")
            .type("java.lang.String")
            .serialisedFormat("format")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
            .parent(new SystemResource().id("/test"));

    private PolicyRequest request;

    @Before
    public void setUp() {
        request = PolicyRequest.Builder.create()
                .withContext(context)
                .withToken(token)
                .withUser(user)
                .withResource(resource);
    }

    @Test
    public void serialiseDeserialiseMaintainsEquality() throws JsonProcessingException {
        // Given there is a request
        LOGGER.info("Request: {}", request);

        // When it is serialised
        String serialised = MAPPER.writeValueAsString(request);
        LOGGER.info("Serialised request: {}", serialised);
        // Then it should serialise successfully

        // When it is deserialised
        PolicyRequest deserialised = MAPPER.readValue(serialised, PolicyRequest.class);
        LOGGER.info("Deserialised request: {}", deserialised);
        // Then it should deserialise successfully

        // When the request has been serialised and deserialised
        // Then it should be equal to the initial object
        assertThat(deserialised, equalTo(request));
    }

    @Test
    public void jsonContainsAllComponents() throws JsonProcessingException {
        // Given fromComponents is built from some number of component objects
        Set<Object> components = new HashSet<>(Arrays.asList(context, token, user, resource));

        // When it is serialised
        String serialised = MAPPER.writeValueAsString(request);
        LOGGER.info("Serialised response is: {}", serialised);

        // Then the serialised string contains the serialised strings of all component objects
        components.stream()
                .map(value -> {
                    try {
                        return MAPPER.writeValueAsString(value);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(serialisedComponent -> {
                    LOGGER.info("Searching for component: {}", serialisedComponent);
                    assertThat(serialised, containsString(serialisedComponent));
                });
    }
}
