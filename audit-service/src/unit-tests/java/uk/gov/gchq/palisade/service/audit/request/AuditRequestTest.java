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

package uk.gov.gchq.palisade.service.audit.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class AuditRequestTest {

    public final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testRegisterRequestCompleteAuditRequest() {
        final AuditRequest.RegisterRequestCompleteAuditRequest subject = AuditRequest.RegisterRequestCompleteAuditRequest.create(new RequestId().id("456"))
                .withUser(new User().userId("a user"))
                .withLeafResources(Stream.of(new FileResource()).collect(toSet()))
                .withContext(new Context(Stream.of(new SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue))));

        assertThat(subject.user.getUserId().getId()).isEqualTo("a user");
    }

    @Test
    public void testRegisterRequestCompleteAuditRequestToJson() throws IOException {
        final LeafResource leafResource = (LeafResource) ResourceBuilder.create("file:/usr/share/resource/test_resource");
        final AuditRequest.RegisterRequestCompleteAuditRequest subject = AuditRequest.RegisterRequestCompleteAuditRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("user"))
                .withLeafResources(Collections.singleton(leafResource.type("standard").serialisedFormat("none").connectionDetail(new SimpleConnectionDetail().serviceName("data-service"))))
                .withContext(new Context(Collections.singletonMap("a string", String.class)));

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat(StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", "))).isEqualTo("class, id, originalRequestId, user, leafResources, context, timestamp, serverIp, serverHostname");
    }

    @Test
    public void testRegisterRequestCompleteAuditRequestFromJson() throws IOException {
        final AuditRequest.RegisterRequestCompleteAuditRequest subject = AuditRequest.RegisterRequestCompleteAuditRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("user"))
                .withLeafResources(Stream.of(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share")))).collect(toSet()))
                .withContext(new Context(Stream.of(new SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue))));

        final String jsonString = "{\"class\":\"AuditRequest$RegisterRequestCompleteAuditRequest\",\"id\":{\"id\":\"5942c37d-43e3-419c-bf1c-bb7153d395c8\"},\"originalRequestId\":{\"id\":\"123\"},\"user\":{\"userId\":{\"id\":\"user\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"},\"leafResources\":[{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"/usr/share/resource/test_resource\",\"attributes\":{},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"resource\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"share\"}},\"serialisedFormat\":\"none\",\"type\":\"standard\"}],\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"a string\":\"java.lang.String\"}}}";

        final AuditRequest.RegisterRequestCompleteAuditRequest result = this.mapper.readValue(jsonString, AuditRequest.RegisterRequestCompleteAuditRequest.class);

        assertThat(subject.context.getContents().keySet().stream().findFirst().orElse("notFound")).isEqualTo("a string");
    }

}
