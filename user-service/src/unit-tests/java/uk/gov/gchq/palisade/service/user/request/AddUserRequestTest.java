/*
 * Copyright 2020 Crown Copyright
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
import uk.gov.gchq.palisade.User;

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
public class AddUserRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(AddUserRequest.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<String> getMessages(final Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }


    @Test
    public void AddUserRequestTest() {
        final AddUserRequest subject = AddUserRequest.create(new RequestId().id("newId")).withUser(new User().userId("newUser"));
        assertThat("AddUserRequest not constructed", subject.user.getUserId().getId(), is(equalTo("newUser")));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("AddUserRequest.create with requestId"),
                Matchers.anyOf(
                        Matchers.containsString("AddUserRequest with originalRequestId"))
        ));
    }

    @Test
    public void AddUserRequestToJsonTest() throws IOException {
        final AddUserRequest subject = AddUserRequest.create(new RequestId().id("newId"))
                .withUser(new User().userId("user"));

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat("AddUserRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("originalRequestId, user, id")));
    }

    @Test
    public void AddUserRequestFromJsonTest() throws IOException {
        final AddUserRequest subject = AddUserRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("user"));

        final String jsonString = "{\"id\":{\"id\":\"3c6324a2-3dfa-43c8-9d96-576b558e2169\"},\"originalRequestId\":{\"id\":\"123\"},\"user\":{\"userId\":{\"id\":\"user\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"}}}";
        final String asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject)).toString();

        final AddUserRequest result = this.mapper.readValue(jsonString, AddUserRequest.class);

        assertThat("AddUserRequest could not be parsed from json string", subject.user, is(equalTo(new User().userId("user"))));
    }
}
