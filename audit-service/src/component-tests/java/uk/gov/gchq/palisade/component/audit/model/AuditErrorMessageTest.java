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
package uk.gov.gchq.palisade.component.audit.model;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.audit.AbstractSerialisationTest;
import uk.gov.gchq.palisade.service.audit.common.Context;
import uk.gov.gchq.palisade.service.audit.model.AuditErrorMessage;

import java.time.ZonedDateTime;
import java.util.Map;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

class AuditErrorMessageTest extends AbstractSerialisationTest {

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     * Takes the JSON Object, deserialises and tests against the original Object
     *
     * @throws Exception if any error occurs during (de)serialisation
     * @see AbstractSerialisationTest#testInstance(Object)
     */
    @Test
    void testAuditErrorMessageSerialisation() throws Exception {

        // GIVEN the instance to check

        var expected = AuditErrorMessage.Builder.create()
            .withUserId("originalUserID")
            .withResourceId("testResourceId")
            .withContext(new Context().purpose("testContext"))
            .withServiceName("testServiceName")
            .withTimestamp(ZonedDateTime.now(UTC).format(ISO_INSTANT))
            .withServerIp("testServerIP")
            .withServerHostname("testServerHostname")
            .withAttributes(Map.of("messagesSent", "23"))
            .withError(new InternalError("Something went wrong!"));

        // THEN confirm that it can be serialised and then deserialised successfully.

        assertSerialisation(expected.getClass(), expected);

    }
}
