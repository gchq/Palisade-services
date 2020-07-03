/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.palisade.response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;


import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class AuditMessageTest {

    @Autowired
    private JacksonTester<AuditMessage> jacksonTester;

    /**
     * Create the AuditMessage object using the builder and then serialise it to a Json string. Test the content of
     * the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {

        Context context = new Context().purpose("testContext");

        AuditMessage auditMessage = AuditMessage.Builder.create()
                .withTimeStamp("testTimeStamp")
                .withServerIp("testServerIP")
                .withServerHostname("testServerHoseName")
                .withContext(context)
                .withUserId("testUserId")
                .withResourceId("testResourceId")
                .withErrorMessage(null);

        JsonContent<AuditMessage> auditMessageJsonContent = jacksonTester.write(auditMessage);

        assertThat(auditMessageJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("testUserId");
        assertThat(auditMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");

    }

    /**
     * Create the AuditMessage object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToResourceResponse() throws IOException {


        String jsonString = "{\"timeStamp\":\"testTimeStamp\",\"serverIp\":\"testServerIP\",\"serverHostname\":\"testServerHoseName\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"userId\":\"testUserId\",\"resourceId\":\"testResourceId\",\"errorMessage\":null}";
        ObjectContent<AuditMessage> auditMessageObjectContent = jacksonTester.parse(jsonString);

        AuditMessage auditMessage = auditMessageObjectContent.getObject();
        assertThat(auditMessage.context.getPurpose()).isEqualTo("testContext");
        assertThat(auditMessage.userId).isEqualTo("testUserId");
        assertThat(auditMessage.resourceId).isEqualTo("testResourceId");

    }


}
