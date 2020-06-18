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
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class PolicyResponseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyResponseTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Context context = new Context().purpose("test-purpose");
    private static final String token = "test-token";
    private static final User user = new User().userId("test-userId");
    private static final LeafResource resource = new FileResource().id("/test/file.format")
            .type("java.lang.String")
            .serialisedFormat("format")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
            .parent(new SystemResource().id("/test"));
    private static final Rules<?> rules = new Rules<>().rule("test-rule", new PassThroughRule<>());

    private PolicyRequest request;
    private PolicyResponse fromComponents;
    private PolicyResponse fromRequest;

    @Before
    public void setUp() {
        request = PolicyRequest.Builder.create()
                .withContext(context)
                .withToken(token)
                .withUser(user)
                .withResource(resource);

        fromComponents = PolicyResponse.Builder.create()
                .withContext(context)
                .withToken(token)
                .withUser(user)
                .withResource(resource)
                .withRules(rules);

        fromRequest = PolicyResponse.Builder.create(request)
                .withRules(rules);
    }

    @Test
    public void fromComponentsEqualsFromRequest() throws JsonProcessingException {
        // Given fromComponents and fromRequest are constructed from the same bits
        // When they are compared
        LOGGER.info("Response from components: {}", fromComponents);
        LOGGER.info("Response from request: {}", fromRequest);
        // Then they are equal
        assertThat(fromComponents, equalTo(fromRequest));

        // When they are serialised
        String fromComponentsSerialised = MAPPER.writeValueAsString(fromComponents);
        String fromRequestSerialised = MAPPER.writeValueAsString(fromRequest);
        LOGGER.info("Serialised response from components: {}", fromComponentsSerialised);
        LOGGER.info("Serialised response from request: {}", fromRequestSerialised);
        // Then they are equal
        assertThat(fromComponentsSerialised, equalTo(fromRequestSerialised));

        // When they are deserialised
        PolicyResponse fromComponentsDeserialised = MAPPER.readValue(fromComponentsSerialised, PolicyResponse.class);
        PolicyResponse fromRequestDeserialised = MAPPER.readValue(fromRequestSerialised, PolicyResponse.class);
        LOGGER.info("Deserialised response from components: {}", fromComponentsDeserialised);
        LOGGER.info("Deserialised response from request: {}", fromRequestDeserialised);
        // Then they are equal
        assertThat(fromComponentsDeserialised, equalTo(fromRequestDeserialised));

        // When the responses have been serialised and deserialised
        // Then they should be equal to their initial objects
        assertThat(fromComponentsDeserialised, equalTo(fromComponents));
        assertThat(fromRequestDeserialised, equalTo(fromRequest));
    }

    @Test
    public void jsonContainsAllComponents() throws JsonProcessingException {
        // Given fromComponents is built from some number of component objects
        Set<Object> components = new HashSet<>(Arrays.asList(context, token, user, resource, rules));

        // When it is serialised
        String serialised = MAPPER.writeValueAsString(fromComponents);
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
