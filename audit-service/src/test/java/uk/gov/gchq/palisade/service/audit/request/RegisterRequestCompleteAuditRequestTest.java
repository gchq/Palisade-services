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

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

@RunWith(JUnit4.class)
public class RegisterRequestCompleteAuditRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(RegisterRequestCompleteAuditRequest.class);
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
    public void RegisterRequestCompleteAuditRequestTest() {
        final RegisterRequestCompleteAuditRequest subject = RegisterRequestCompleteAuditRequest.create(new RequestId().id("456"))
                .withUser(new User().userId("a user"))
                .withLeafResources(Stream.of(new FileResource()).collect(toSet()))
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))));

        assertThat("RegisterRequestCompleteAuditRequest not constructed", subject.user.getUserId().getId(), is(equalTo("a user")));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("RegisterRequestCompleteAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("RegisterRequestCompleteAuditRequest called with originalRequestId"))
        ));
    }

    @Test
    public void RegisterRequestCompleteAuditRequestToJsonTest() throws IOException {
        final RegisterRequestCompleteAuditRequest subject = RegisterRequestCompleteAuditRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("user"))
                .withLeafResources(Stream.of(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share")))).collect(toSet()))
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))));

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat("RegisterRequestCompleteAuditRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("class, id, originalRequestId, user, leafResources, context, timestamp, serverIp, serverHostname")));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("RegisterRequestCompleteAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("RegisterRequestCompleteAuditRequest called with originalRequestId"))
        ));
    }

    @Test
    public void RegisterRequestCompleteAuditRequestFromJsonTest() throws IOException {
        final RegisterRequestCompleteAuditRequest subject = RegisterRequestCompleteAuditRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("user"))
                .withLeafResources(Stream.of(new FileResource().id("/usr/share/resource/test_resource").type("standard").serialisedFormat("none").parent(new DirectoryResource().id("resource").parent(new SystemResource().id("share")))).collect(toSet()))
                .withContext(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))));

        final String jsonString = "{\"class\":\"RegisterRequestCompleteAuditRequest\",\"id\":{\"id\":\"5942c37d-43e3-419c-bf1c-bb7153d395c8\"},\"originalRequestId\":{\"id\":\"123\"},\"user\":{\"userId\":{\"id\":\"user\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"},\"leafResources\":[{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"/usr/share/resource/test_resource\",\"attributes\":{},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"resource\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"share\"}},\"serialisedFormat\":\"none\",\"type\":\"standard\"}],\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"a string\":\"java.lang.String\"}}}";

        final RegisterRequestCompleteAuditRequest result = this.mapper.readValue(jsonString, RegisterRequestCompleteAuditRequest.class);

        assertThat("RegisterRequestCompleteAuditRequest could not be parsed from json string", subject.context.getContents().keySet().stream().findFirst().orElse("notFound"), is(equalTo("a string")));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages, is(not(emptyList())));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("RegisterRequestCompleteAuditRequest.create called with RequestId"),
                Matchers.anyOf(
                        Matchers.containsString("RegisterRequestCompleteAuditRequest called with originalRequestId"))
        ));

    }
}