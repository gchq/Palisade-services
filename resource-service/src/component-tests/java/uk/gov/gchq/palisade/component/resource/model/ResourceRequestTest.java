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
package uk.gov.gchq.palisade.component.resource.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.resource.model.ResourceRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = {ResourceRequestTest.class})
class ResourceRequestTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void testResourceRequestSerializingAndDeseralizing() throws IOException {
        ResourceRequest resourceRequest = ResourceRequest.Builder.create()
                .withUserId("originalUserId")
                .withResourceId("testResourceId")
                .withContext(new Context().purpose("testContext"))
                .withUser(new User().userId("testUserId"));

        var actualJson = mapper.writeValueAsString(resourceRequest);
        var actualInstance = mapper.readValue(actualJson, resourceRequest.getClass());

        assertThat(actualInstance)
                .as("Check using recursion that the %s has been deseralized successfully", resourceRequest.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(resourceRequest);
    }
}
