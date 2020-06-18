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

public class PolicyMessagesTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyMessagesTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Context context = new Context().purpose("test-purpose");
    private final String token = "test-token";
    private final User user = new User().userId("test-userId");
    private final LeafResource resource = new FileResource().id("/test/file.format")
            .type("java.lang.String")
            .serialisedFormat("format")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
            .parent(new SystemResource().id("/test"));
    private final Rules<?> rules = new Rules<>().rule("test-rule", new PassThroughRule<>());

    @Test
    public void loggingInspectionTest() throws JsonProcessingException {
        PolicyRequest request = PolicyRequest.Builder.create()
                .withContext(context)
                .withToken(token)
                .withUser(user)
                .withResource(resource);

        LOGGER.info("Request is {}", request);
        LOGGER.info("JSON is {}", MAPPER.writeValueAsString(request));

        PolicyResponse response = PolicyResponse.Builder.create(request)
                .withRules(rules);

        LOGGER.info("Response is {}", response);
        LOGGER.info("JSON is {}", MAPPER.writeValueAsString(response));
    }
}
