package uk.gov.gchq.palisade.service.user.request;


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
import uk.gov.gchq.palisade.UserId;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotEquals;

@RunWith(JUnit4.class)
public class GetUserRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(GetUserRequest.class);
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
    public void GetUserRequestTest() {
        final GetUserRequest subject = GetUserRequest.create(new RequestId().id("newId")).withUserId(new UserId().id("newUser"));
        assertThat("GetUserRequest not constructed", subject.userId.getId(), is(equalTo("newUser")));
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("GetUserRequest.create with requestId"),
                Matchers.anyOf(
                        Matchers.containsString("GetUserRequest with requestId"))
        ));
    }

    @Test
    public void GetUserRequestToJsonTest() throws IOException {
        final GetUserRequest subject = GetUserRequest.create(new RequestId().id("newId"))
                .withUserId(new UserId().id("user"));

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat("GetUserRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("id, originalRequestId, userId")));
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("GetUserRequest.create with requestId"),
                Matchers.anyOf(
                        Matchers.containsString("GetUserRequest with requestId"))
        ));
    }

    @Test
    public void GetUserRequestFromJsonTest() throws IOException {
        final GetUserRequest subject = GetUserRequest.create(new RequestId().id("123"))
                .withUserId(new UserId().id("newUser"));

        final String jsonString = "{\"id\":{\"id\":\"9b3b4751-d88d-4aad-9a59-022fb76e8474\"},\"originalRequestId\":{\"id\":\"123\"},\"userId\":{\"id\":\"newUser\"}}";

        assertThat("GetUserRequest could not be parsed from json string", subject.userId, is(equalTo(new UserId().id("newUser"))));
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("GetUserRequest.create with requestId"),
                Matchers.anyOf(
                        Matchers.containsString("GetUserRequest with requestId"))
        ));
    }
}
