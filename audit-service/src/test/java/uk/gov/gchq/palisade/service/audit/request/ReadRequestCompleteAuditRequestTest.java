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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

@RunWith(JUnit4.class)
public class ReadRequestCompleteAuditRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(ReadRequestCompleteAuditRequest.class);
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
    public void ReadRequestCompleteAuditRequestFromJsonTest() throws IOException {
        final ReadRequestCompleteAuditRequest subject = ReadRequestCompleteAuditRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("user"))
                .withLeafResource(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share"))))
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .withRulesApplied(new Rules().message("new Rule"))
                .withNumberOfRecordsReturned(100L)
                .withNumberOfRecordsProcessed(200L);

        assertThat("ReadRequestCompleteAuditRequest could not be parsed from json string", subject.numberOfRecordsProcessed, is(equalTo(200L)));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("ReadRequestCompleteAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("ReadRequestCompleteAuditRequest called with originalRequestId"))
        ));
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
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("ReadRequestCompleteAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("ReadRequestCompleteAuditRequest called with originalRequestId"))
        ));
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
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("ReadRequestCompleteAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("ReadRequestCompleteAuditRequest called with originalRequestId"))
        ));
    }

}