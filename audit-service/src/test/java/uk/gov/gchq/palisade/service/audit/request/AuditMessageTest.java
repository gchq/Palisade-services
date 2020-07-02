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
package uk.gov.gchq.palisade.service.audit.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.audit.request.common.domain.User;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@JsonTest
public class AuditMessageTest {

    @Autowired
    private JacksonTester<AuditMessage> jacksonTester;

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
        Rules<Resource> rules = new Rules<>();
        AuditMessage auditMessage = AuditMessage.Builder.create()
                .withTimeStamp("testTimeStamp")
                .withServerIp("testServerIP")
                .withServerHostname("testServerHoseName")
                .withContext(context)
                .withUserId("testUserID")
                .withUser(user)
                .withResourceId("testResourceId")
                .withResource(resource)
                .withRules(rules)
                .withRecordsReturned(42)
                .withRecordsProcessed(37)
                .withErrorMessage(null);

        JsonContent<AuditMessage> auditRequest2JsonContent = jacksonTester.write(auditMessage);

        assertThat(auditRequest2JsonContent).extractingJsonPathStringValue("$.user.user_id").isEqualTo("testUserId");
        assertThat(auditRequest2JsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");

    }

    /**
     * Create the ResourceResponse object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToResourceResponse() throws IOException {


        String jsonString = "{\"timeStamp\":\"testTimeStamp\",\"serverIp\":\"testServerIP\",\"serverHostname\":\"testServerIP\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"userId\":\"testUserID\",\"user\":{\"user_id\":\"testUserId\",\"attributes\":{}},\"resourceId\":\"testResourceId\",\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"testResourceId\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"/test/\"},\"serialisedFormat\":\"format\",\"type\":\"java.lang.String\"},\"rules\":{\"message\":\"no rules set\",\"rules\":{}},\"numberOfRecordsReturned\":42,\"numberOfRecordsProcessed\":37,\"errorMessage\":null}";
        ObjectContent<AuditMessage> auditRequest2ObjectContent =  jacksonTester.parse(jsonString);

        AuditMessage auditMessage =  auditRequest2ObjectContent.getObject();
        assertThat(auditMessage.context.getPurpose()).isEqualTo("testContext");
        assertThat(auditMessage.user.userId).isEqualTo("testUserId");
        assertThat(auditMessage.resource.getId()).isEqualTo("testResourceId");
        assertThat(auditMessage.numberOfRecordsProcessed).isEqualTo(37);

    }


}
