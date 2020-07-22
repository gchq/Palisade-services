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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class AuditSuccessMessageTest {

    @Autowired
    private JacksonTester<AuditSuccessMessage> jsonTester;

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     *
     * @throws IOException throws if the UserResponse object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise the string.
     */
    @Test
    public void testSerialiseUserResponseToJson() throws IOException {
        Context context = new Context().purpose("testContext");
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("messagesSent", "23");

        AuditSuccessMessage auditSuccessMessage = AuditSuccessMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(context)
                .withServiceName("testServicename")
                .withTimestamp(now)
                .withServerIp("testServerIP")
                .withServerHostname("testServerHostname")
                .withAttributes(attributes)
                .withLeafResourceId("testLeafResourceId");

        JsonContent<AuditSuccessMessage> auditSuccessMessageJsonContent = jsonTester.write(auditSuccessMessage);

        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID");
        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");
        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.serviceName").isEqualTo("testServicename");
        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.timestamp").isEqualTo(now);
        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.serverIP").isEqualTo("testServerIP");
        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.serverHostname").isEqualTo("testServerHostname");
        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.attributes.messagesSent").isEqualTo("23");
        assertThat(auditSuccessMessageJsonContent).extractingJsonPathStringValue("$.leafResourceId").isEqualTo("testLeafResourceId");
    }

    /**
     * Create the object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testDeserialiseJsonToUserResponse() throws IOException {
        String jsonString = "{\"userId\":\"originalUserID\",\"resourceId\":\"testResourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":" +
                "{\"purpose\":\"testContext\"}},\"serviceName\":\"audit-service\",\"timestamp\":\"2020-07-14T09:56:08.244549Z\",\"serverIP\":\"192.168.1.1\"," +
                "\"serverHostname\":\"host.name\",\"attributes\":{\"messagesSent\":\"23\"},\"leafResourceId\":\"testLeafResourceId\",\"serveHostName\":\"host.name\"}";

        ObjectContent<AuditSuccessMessage> auditSuccessMessageObjectContent = jsonTester.parse(jsonString);

        AuditSuccessMessage auditSuccessMessage = auditSuccessMessageObjectContent.getObject();
        assertThat(auditSuccessMessage.getUserId()).isEqualTo("originalUserID");
        assertThat(auditSuccessMessage.getResourceId()).isEqualTo("testResourceId");
        assertThat(auditSuccessMessage.getContext().getPurpose()).isEqualTo("testContext");
        assertThat(auditSuccessMessage.getServiceName()).isEqualTo("audit-service");
        assertThat(auditSuccessMessage.getServerIP()).isEqualTo("192.168.1.1");
        assertThat(auditSuccessMessage.getServeHostName()).isEqualTo("host.name");
        assertThat(auditSuccessMessage.getAttributes().get("messagesSent")).isEqualTo("23");
        assertThat(auditSuccessMessage.getLeafResourceId()).isEqualTo("testLeafResourceId");
    }
}
