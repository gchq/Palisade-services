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
import uk.gov.gchq.palisade.service.audit.service.AuditService;

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
public class RegisterRequestExceptionAuditRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(RegisterRequestExceptionAuditRequest.class);
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
    public void RegisterRequestExceptionAuditRequestTest() {
        final RegisterRequestExceptionAuditRequest subject = RegisterRequestExceptionAuditRequest.create(new RequestId().id("304958"))
                .withUserId(new User().userId("username").getUserId())
                .withResourceId("resource")
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a reason for access", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .withException(new SecurityException("not allowed"))
                .withServiceClass(AuditService.class);

        assertThat("RegisterRequestExceptionAuditRequest not constructed", subject.resourceId, is(equalTo("resource")));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("RegisterRequestExceptionAuditRequest.create called with originalRequestId"),
                Matchers.anyOf(
                        Matchers.containsString("RegisterRequestExceptionAuditRequest called with originalRequestId"))
        ));
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

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("RegisterRequestExceptionAuditRequest.create called with originalRequestId"),
                Matchers.anyOf(
                        Matchers.containsString("RegisterRequestExceptionAuditRequest called with originalRequestId"))
        ));
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

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("RegisterRequestExceptionAuditRequest.create called with originalRequestId"),
                Matchers.anyOf(
                        Matchers.containsString("RegisterRequestExceptionAuditRequest called with originalRequestId"))
        ));
    }

}