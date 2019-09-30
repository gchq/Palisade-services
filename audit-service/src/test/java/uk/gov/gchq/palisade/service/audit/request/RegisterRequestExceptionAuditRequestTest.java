package uk.gov.gchq.palisade.service.audit.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.audit.AuditService;

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
public class RegisterRequestExceptionAuditRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();


    @Test
    public void RegisterRequestExceptionAuditRequestTest() {
        final RegisterRequestExceptionAuditRequest subject = RegisterRequestExceptionAuditRequest.create(new RequestId().id("304958"))
                .withUserId(new User().userId("username").getUserId())
                .withResourceId("resource")
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a reason for access", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .withException(new SecurityException("not allowed"))
                .withServiceClass(AuditService.class);

        assertThat("RegisterRequestExceptionAuditRequest not constructed", subject.resourceId, is(equalTo("resource")));
    }

    @Test
    public void RegisterRequestExceptionAuditRequestToJsonTest() throws IOException {
        final RegisterRequestExceptionAuditRequest subject = RegisterRequestExceptionAuditRequest.create(new RequestId().id("456"))
                .withUserId(new User().userId("user2").getUserId())
                .withResourceId("resourcful")
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a reason for access", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .withException(new SecurityException("super not allowed"))
                .withServiceClass(AuditService.class);

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat("RegisterRequestExceptionAuditRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("class, id, originalRequestId, userId, resourceId, context, exception, serviceClass, timestamp, serverIp, serverHostname")));
    }

    @Test
    public void RegisterRequestExceptionAuditRequestFromJsonTest() throws IOException {
        final RegisterRequestExceptionAuditRequest subject = RegisterRequestExceptionAuditRequest.create(new RequestId().id("789"))
                .withUserId(new User().userId("user").getUserId())
                .withResourceId("resourced")
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .withException(new SecurityException("really not allowed"))
                .withServiceClass(AuditService.class);

        assertThat("RegisterRequestExceptionAuditRequest could not be parsed from json string", subject.context.getContents().keySet().stream().findFirst().orElse("notFound"), is(equalTo("a string")));
    }

}