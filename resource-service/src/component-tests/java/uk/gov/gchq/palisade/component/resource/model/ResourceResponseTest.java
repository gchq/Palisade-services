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
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.resource.model.ResourceResponse;
import uk.gov.gchq.palisade.service.resource.stream.config.AkkaSystemConfig;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ContextConfiguration(classes = {ResourceResponseTest.class, AkkaSystemConfig.class})
class ResourceResponseTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void testSerializeResourceResponseToJson() throws IOException {
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));

        ResourceResponse resourceResponse = ResourceResponse.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("originalResourceID")
                .withContext(new Context().purpose("testContext"))
                .withUser(new User().userId("testUserId"))
                .withResource(resource);

        var actualJson = mapper.writeValueAsString(resourceResponse);
        var actualInstance = mapper.readValue(actualJson, resourceResponse.getClass());

        assertThat(actualInstance)
                .as("Using recursion, check that the %s object has been deserialised successfully", resourceResponse.getClass().getSimpleName())
                .usingRecursiveComparison()
                .isEqualTo(resourceResponse);
    }
}
