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

package uk.gov.gchq.palisade.service.user.web;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.config.StdUserConfiguration;
import uk.gov.gchq.palisade.service.user.model.AddUserRequest;
import uk.gov.gchq.palisade.service.user.model.GetUserRequest;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.MockUserService;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration.Topic;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UserRestControllerTest {
    private final LinkedList<ProducerRecord<String, UserRequest>> sinkAggregation = new LinkedList<>();
    private final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> aggregatorSink = Sink.foreach(sinkAggregation::addLast);
    private final ConsumerTopicConfiguration mockTopicConfig = Mockito.mock(ConsumerTopicConfiguration.class);
    private final Topic mockTopic = Mockito.mock(Topic.class);
    private final Materializer materializer = Materializer.createMaterializer(ActorSystem.create());
    public final UserRestController userRestController = new UserRestController(
            new MockUserService(), new StdUserConfiguration(),
            aggregatorSink, mockTopicConfig, materializer);

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(UserRestController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
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
    void testAddAndGetUser() {
        User user = new User().userId("add-user-request-id").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));
        AddUserRequest addUserRequest = AddUserRequest.create(new RequestId().id("addUserRequest")).withUser(user);
        Boolean addedUser = userRestController.addUserRequest(addUserRequest);

        assertThat(addedUser).isTrue();

        GetUserRequest getUserRequest = GetUserRequest.create(new RequestId().id("getUserRequest")).withUserId(user.getUserId());
        User expected = userRestController.getUserRequest(getUserRequest);
        assertThat(user).isEqualTo(expected);

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.INFO);
        assertThat(debugMessages).isNotEmpty();
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Invoking AddUserRequest:"),
                Matchers.anyOf(
                        Matchers.containsString("Invoking GetUserRequest: GetUserRequest"))
        ));
    }
}