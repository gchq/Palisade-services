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
package uk.gov.gchq.palisade.component.attributemask.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.User;
import uk.gov.gchq.palisade.service.attributemask.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.common.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeMaskingRequestTest {

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws JsonProcessingException throws if the {@link AttributeMaskingRequest} object cannot be converted to a JsonContent.
     *                                 This equates to a failure to serialise or deserialise the string.
     */
    @Test
    void testGroupedDependantAttributeMaskingRequestSerialisingAndDeserialising() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));

        var attributeMaskingRequest = AttributeMaskingRequest.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(new Context().purpose("testContext"))
                .withUser(new User().userId("testUserId"))
                .withResource(resource)
                .withRules(new Rules<>());

        var actualJson = mapper.writeValueAsString(attributeMaskingRequest);
        var actualInstance = mapper.readValue(actualJson, attributeMaskingRequest.getClass());

        assertThat(actualInstance)
                .as("Check that whilst using the objects toString method, the objects are the same")
                .isEqualTo(attributeMaskingRequest);

        assertThat(actualInstance)
                .as("Check %s using recursion)", attributeMaskingRequest.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(attributeMaskingRequest);

    }
}
