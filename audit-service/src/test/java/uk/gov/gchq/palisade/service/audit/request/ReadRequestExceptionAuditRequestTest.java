package uk.gov.gchq.palisade.service.audit.request;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotEquals;

@RunWith(JUnit4.class)
public class ReadRequestExceptionAuditRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(ReadRequestExceptionAuditRequest.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<String> getMessages(Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }


    @Test
    public void ReadRequestExceptionAuditRequestFromJsonTest() throws IOException {
        final ReadRequestExceptionAuditRequest subject = ReadRequestExceptionAuditRequest.create(new RequestId().id("123"))
                .withToken("789")
                .withLeafResource(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share"))))
                .withException(new SecurityException("not allowed"));

        assertThat("ReadRequestExceptionAuditRequest could not be parsed from json string", subject.exception.getLocalizedMessage(), is(equalTo("not allowed")));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("ReadRequestExceptionAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("ReadRequestExceptionAuditRequest called with originalRequestId"))
        ));
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

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("ReadRequestExceptionAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("ReadRequestExceptionAuditRequest called with originalRequestId"))
        ));
    }

    @Test
    public void ReadRequestExceptionAuditRequestTest() {
        final ReadRequestExceptionAuditRequest subject = ReadRequestExceptionAuditRequest.create(new RequestId().id("456"))
                .withToken("token")
                .withLeafResource(new FileResource())
                .withException(new SecurityException("not allowed"));

        assertThat("ReadRequestExceptionAuditRequest not constructed", subject.token, is(equalTo("token")));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("ReadRequestExceptionAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("ReadRequestExceptionAuditRequest called with originalRequestId"))
        ));
    }
}