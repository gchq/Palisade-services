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
package uk.gov.gchq.palisade.service.data.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.data.request.AuditRequest.ReadRequestCompleteAuditRequest;
import uk.gov.gchq.palisade.service.data.request.AuditRequest.ReadRequestExceptionAuditRequest;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(JUnit4.class)
public class AuditRequestTest {

    private ObjectMapper mapper;
    private static LeafResource resource = new FileResource()
            .id("/docs/type_file.txt")
            .serialisedFormat("txt")
            .type("type")
            .parent(new DirectoryResource().id("/docs").parent(new SystemResource().id("/")));
    private static Rules<Resource> rules = new Rules<>();

    @Before
    public void setup() {
        mapper = JSONSerialiser.createDefaultMapper();
    }

    @Test
    public void ReadRequestCompleteAuditRequestTest() {
        final ReadRequestCompleteAuditRequest subject = AuditRequest.ReadRequestCompleteAuditRequest.create(new RequestId().id("456"))
                .withUser(new User().userId("a user"))
                .withLeafResource(resource)
                .withContext(new Context(Stream.of(new SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue))))
                .withRulesApplied(rules)
                .withNumberOfRecordsReturned(1L)
                .withNumberOfRecordsProcessed(1L);

        assertThat("RegisterRequestCompleteAuditRequest not constructed", subject.user.getUserId().getId(), is(equalTo("a user")));
    }

    @Test
    public void ReadRequestCompleteAuditRequestToJsonTest() throws IOException {
        final ReadRequestCompleteAuditRequest subject = AuditRequest.ReadRequestCompleteAuditRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("a user"))
                .withLeafResource(resource)
                .withContext(new Context(Stream.of(new SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue))))
                .withRulesApplied(rules)
                .withNumberOfRecordsReturned(1L)
                .withNumberOfRecordsProcessed(1L);

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        System.out.println(asNode);

        assertThat("RegisterRequestCompleteAuditRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("class, id, originalRequestId, user, leafResource, context, rulesApplied, numberOfRecordsReturned, numberOfRecordsProcessed, timestamp, serverIp, serverHostname")));
    }

    @Test
    public void ReadRequestCompleteAuditRequestFromJsonTest() throws IOException {
        final ReadRequestCompleteAuditRequest subject = AuditRequest.ReadRequestCompleteAuditRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("a user"))
                .withLeafResource(resource)
                .withContext(new Context(Stream.of(new SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue))))
                .withRulesApplied(rules)
                .withNumberOfRecordsReturned(1L)
                .withNumberOfRecordsProcessed(1L);

        final String jsonString = "{\"class\":\"AuditRequest$ReadRequestCompleteAuditRequest\",\"id\":{\"id\":\"3d911110-4ea3-4853-936e-2386a0fab799\"},\"originalRequestId\":{\"id\":\"123\"},\"user\":{\"userId\":{\"id\":\"a user\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"},\"leafResource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"/docs/type_file.txt\",\"attributes\":{},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"/docs/\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"/\"}},\"serialisedFormat\":\"txt\",\"type\":\"type\"},\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"a string\":\"java.lang.String\"}},\"rulesApplied\":{\"message\":\"no rules set\",\"rules\":{}},\"numberOfRecordsReturned\":1,\"numberOfRecordsProcessed\":1}";

        final ReadRequestCompleteAuditRequest result = this.mapper.readValue(jsonString, ReadRequestCompleteAuditRequest.class);

        assertThat("RegisterRequestCompleteAuditRequest could not be parsed from json string", subject.context.getContents().keySet().stream().findFirst().orElse("notFound"), is(equalTo("a string")));
    }

    @Test
    public void ReadRequestExceptionAuditRequestTest() {
        final ReadRequestExceptionAuditRequest subject = ReadRequestExceptionAuditRequest.create(new RequestId().id("304958"))
                .withToken("token")
                .withLeafResource(resource)
                .withException(new SecurityException("not allowed"));

        assertThat("ReadRequestExceptionAuditRequest not constructed", subject.leafResource, is(equalTo(resource)));
    }

}
