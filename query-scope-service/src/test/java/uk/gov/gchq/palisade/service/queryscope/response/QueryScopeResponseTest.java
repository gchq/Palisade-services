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
package uk.gov.gchq.palisade.service.queryscope.response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.queryscope.response.common.domain.User;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class QueryScopeResponseTest {

    @Autowired
    private JacksonTester<QueryScopeResponse> jacksonTester;


    /**
     * Create the object using the builder and then serialise it to a Json string. Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {

        Context context = new Context().purpose("testContext");
        User user = User.create("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        Rules rules = new Rules().rule("Rule1", new PassThroughRule());
        QueryScopeResponse queryScopeResponse = QueryScopeResponse.Builder.create().withContext(context).withUser(user).withResource(resource).withRules(rules);

        JsonContent<QueryScopeResponse> queryScopeResponseJsonContent = jacksonTester.write(queryScopeResponse);

        assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.user.user_id").isEqualTo("testUserId");
        assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(queryScopeResponseJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format");

    }

    /**
     * Create the ResourceResponse object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToResourceResponse() throws IOException {

        String jsonString = "{\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"user\":{\"user_id\":\"testUserId\",\"attributes\":{}},\"resources\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"/test/file.format\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"/test/\"},\"serialisedFormat\":\"format\",\"type\":\"java.lang.String\"},\"rules\":{\"message\":\"no rules set\",\"rules\":{\"Rule1\":{\"class\":\"uk.gov.gchq.palisade.policy.PassThroughRule\"}}}}";

        ObjectContent<QueryScopeResponse> queryScopeResponseObjectContentObjectContent = jacksonTester.parse(jsonString);

        QueryScopeResponse queryScopeResponse = queryScopeResponseObjectContentObjectContent.getObject();
         assertThat(queryScopeResponse.getContext().getPurpose()).isEqualTo("testContext");
        assertThat(queryScopeResponse.getUser().userId).isEqualTo("testUserId");
        assertThat(queryScopeResponse.getResource().getId()).isEqualTo("/test/file.format");

    }
}

