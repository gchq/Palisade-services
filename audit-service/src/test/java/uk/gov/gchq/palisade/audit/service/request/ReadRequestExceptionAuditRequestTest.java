package uk.gov.gchq.palisade.audit.service.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(JUnit4.class)
public class ReadRequestExceptionAuditRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();


    @Test
    public void ReadRequestExceptionAuditRequestFromJsonTest() throws IOException {
        final ReadRequestExceptionAuditRequest subject = ReadRequestExceptionAuditRequest.create(new RequestId().id("123"))
                .withToken("789")
                .withLeafResource(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share"))))
                .withException(new SecurityException("not allowed"));

        assertThat("ReadRequestExceptionAuditRequest could not be parsed from json string", subject.exception.getLocalizedMessage(), is(equalTo("not allowed")));
    }


    @Test
    public void ReadRequestExceptionAuditRequestToJsonTest() throws IOException {
        final ReadRequestExceptionAuditRequest subject = ReadRequestExceptionAuditRequest.create(new RequestId().id("123"))
                .withToken("token")
                .withLeafResource((new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share")))))
                .withException(new SecurityException("not allowed"));

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat("ReadRequestExceptionAuditRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("class, id, originalRequestId, token, leafResource, exception, timestamp, serverIp, serverHostname")));
    }

    @Test
    public void ReadRequestExceptionAuditRequestTest() {
        final ReadRequestExceptionAuditRequest subject = ReadRequestExceptionAuditRequest.create(new RequestId().id("456"))
                .withToken("token")
                .withLeafResource(new FileResource())
                .withException(new SecurityException("not allowed"));

        assertThat("ReadRequestExceptionAuditRequest not constructed", subject.token, is(equalTo("token")));
    }
}