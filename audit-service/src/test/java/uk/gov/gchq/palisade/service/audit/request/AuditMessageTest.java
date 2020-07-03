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
    public void testSerialiseAuditMessageToJson() throws IOException {

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

        JsonContent<AuditMessage> auditMessageJsonContent = jacksonTester.write(auditMessage);

        assertThat(auditMessageJsonContent).extractingJsonPathStringValue("$.user.user_id").isEqualTo("testUserId");
        assertThat(auditMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");

    }



    /**
     * Create the AuditMessage object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToAuditMessage() throws IOException {


        String jsonString = "{\"timeStamp\":\"testTimeStamp\",\"serverIp\":\"testServerIP\",\"serverHostname\":\"testServerIP\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"userId\":\"testUserID\",\"user\":{\"user_id\":\"testUserId\",\"attributes\":{}},\"resourceId\":\"testResourceId\",\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"testResourceId\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"/test/\"},\"serialisedFormat\":\"format\",\"type\":\"java.lang.String\"},\"rules\":{\"message\":\"no rules set\",\"rules\":{}},\"numberOfRecordsReturned\":42,\"numberOfRecordsProcessed\":37,\"errorMessage\":null}";
        ObjectContent<AuditMessage> auditMessageObjectContent =  jacksonTester.parse(jsonString);

        AuditMessage auditMessage =  auditMessageObjectContent.getObject();
        assertThat(auditMessage.context.getPurpose()).isEqualTo("testContext");
        assertThat(auditMessage.user.userId).isEqualTo("testUserId");
        assertThat(auditMessage.resource.getId()).isEqualTo("testResourceId");
        assertThat(auditMessage.numberOfRecordsProcessed).isEqualTo(37);

    }



    /**
     * Create the AuditMessage object for the audit-service from a Json string based on the content from an
     * AuditMessage.  This unit test is meant to demonstrate the sending of an AuditMessage from one the
     * different services being sent to the audit-service where it is processed.  In this instance the Json string
     * is the message created in the palisade-service and is then forwarded to the audit-service.  The AuditMessage
     * (for audit-service) should be able to represent the data from this variant (palisade-service) along with all
     * of the other versions from each of the services.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonFromPalisadeServiceToAuditMessage() throws IOException {

        //message content for a message sent from Palisade service.
        String jsonString = "{\"timeStamp\":\"testTimeStamp\",\"serverIp\":\"testServerIP\",\"serverHostname\":\"testServerHoseName\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"userId\":\"testUserId\",\"resourceId\":\"testResourceId\",\"errorMessage\":null}";

        ObjectContent<AuditMessage> auditMessageObjectContent =  jacksonTester.parse(jsonString);

        AuditMessage auditMessage =  auditMessageObjectContent.getObject();
        assertThat(auditMessage.context.getPurpose()).isEqualTo("testContext");
        assertThat(auditMessage.userId).isEqualTo("testUserId");
        assertThat(auditMessage.resourceId).isEqualTo("testResourceId");

        assertThat(auditMessage.user).isNull();
        assertThat(auditMessage.resource).isNull();
        assertThat(auditMessage.rules).isNull();

        assertThat(auditMessage.numberOfRecordsProcessed).isEqualTo(0);


    }

}
