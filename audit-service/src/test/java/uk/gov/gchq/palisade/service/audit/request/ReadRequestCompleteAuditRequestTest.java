package uk.gov.gchq.palisade.service.audit.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(JUnit4.class)
public class ReadRequestCompleteAuditRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();


    @Test
    public void ReadRequestCompleteAuditRequestFromJsonTest() throws IOException {
        final ReadRequestCompleteAuditRequest subject = ReadRequestCompleteAuditRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("user"))
                .withLeafResource(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share"))))
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .withRulesApplied(new Rules().message("new Rule"))
                .withNumberOfRecordsReturned(100L)
                .withNumberOfRecordsProcessed(200L);

        assertThat("ReadRequestCompleteAuditRequest could not be parsed from json string", subject.numberOfRecordsProcessed, is(equalTo(200L)));
    }


    @Test
    public void ReadRequestCompleteAuditRequestToJsonTest() throws IOException {
        final ReadRequestCompleteAuditRequest subject = ReadRequestCompleteAuditRequest.create(new RequestId().id("456"))
               .withUser(new User().userId("user1"))
                .withLeafResource(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share"))))
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .withRulesApplied(new Rules().message("newer Rule"))
                .withNumberOfRecordsReturned(300L)
                .withNumberOfRecordsProcessed(400L);

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat("ReadRequestCompleteAuditRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("class, id, originalRequestId, user, leafResource, context, rulesApplied, numberOfRecordsReturned, numberOfRecordsProcessed, timestamp, serverIp, serverHostname")));
    }

    @Test
    public void ReadRequestCompleteAuditRequestTest() {
        final ReadRequestCompleteAuditRequest subject = ReadRequestCompleteAuditRequest.create(new RequestId().id("789"))
                .withUser(new User().userId("user2"))
                .withLeafResource(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share"))))
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .withRulesApplied(new Rules().message("newest Rule"))
                .withNumberOfRecordsReturned(500L)
                .withNumberOfRecordsProcessed(600L);

        assertThat("ReadRequestCompleteAuditRequest not constructed", subject.user.getUserId().getId(), is(equalTo("user2")));
    }

}