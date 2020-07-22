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
package uk.gov.gchq.palisade.service.results.request;

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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class AuditErrorMessageTest {

    @Autowired
    private JacksonTester<AuditErrorMessage> jsonTester;

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     *
     * @throws IOException throws if the UserResponse object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise the string.
     */
    @Test
    public void testSerialiseAuditErrorMessageToJson() throws IOException {
        Context context = new Context().purpose("testContext");
        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        AuditErrorMessage auditErrorMessage = AuditErrorMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId("testResourceId")
                .withContext(context)
                .withAttributes(new HashMap<String, Object>())
                .withError(new InternalError("Something went wrong!"));

        JsonContent<AuditErrorMessage> auditErrorMessageJsonContent = jsonTester.write(auditErrorMessage);

        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID");
        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");
        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.error.message").isEqualTo("Something went wrong!");
    }

    /**
     * Create the object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testDeserialiseJsonToAuditErrorMessage() throws IOException {
        String jsonString = "{\"userId\":\"originalUserID\",\"resourceId\":\"testResourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":" +
                "{\"purpose\":\"testContext\"}},\"error\":{\"cause\":null,\"stackTrace\":[],\"message\":\"Something went wrong!\",\"suppressed\":[],\"localizedMessage" +
                "\":\"Something went wrong!\"},\"serviceName\":\"results-service\",\"timestamp\":\"2020-01-01T08:00:00.000000Z\",\"serverIP" +
                "\":\"192.168.1.1\",\"serverHostname\":\"host.name\",\"attributes\":{}}";

        ObjectContent<AuditErrorMessage> auditSuccessMessageObjectContent = jsonTester.parse(jsonString);

        AuditErrorMessage auditErrorMessage = auditSuccessMessageObjectContent.getObject();
        assertThat(auditErrorMessage.getUserId()).isEqualTo("originalUserID");
        assertThat(auditErrorMessage.getResourceId()).isEqualTo("testResourceId");
        assertThat(auditErrorMessage.getContext().getPurpose()).isEqualTo("testContext");
        assertThat(auditErrorMessage.getServiceName()).isEqualTo("results-service");
        assertThat(auditErrorMessage.getServerIP()).isEqualTo("192.168.1.1");
        assertThat(auditErrorMessage.getServeHostName()).isEqualTo("host.name");
        assertThat(auditErrorMessage.getError().getMessage()).isEqualTo("Something went wrong!");
    }
}
