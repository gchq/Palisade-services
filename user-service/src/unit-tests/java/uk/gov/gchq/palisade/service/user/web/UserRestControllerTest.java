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

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.UserService;
import uk.gov.gchq.palisade.service.user.service.UserServiceProxy;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.user.stream.ProducerTopicConfiguration.Topic;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UserRestControllerTest {
    private final LinkedList<ProducerRecord<String, UserRequest>> sinkAggregation = new LinkedList<>();
    private final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> aggregatorSink = Sink.foreach(sinkAggregation::addLast);
    private final ConsumerTopicConfiguration mockTopicConfig = Mockito.mock(ConsumerTopicConfiguration.class);
    private final Topic mockTopic = Mockito.mock(Topic.class);
    private final Materializer materializer = Materializer.createMaterializer(ActorSystem.create());
    private final UserService service = Mockito.mock(UserService.class);
    private final UserServiceProxy proxy = new UserServiceProxy(service);

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(UserServiceProxy.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
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
        // Given
        User expected = new User().userId("add-user-request-id").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));
        when(service.addUser(any(User.class))).thenReturn(expected);
        when(service.getUser(any(UserId.class))).thenReturn(expected);

        // When
        User addedUser = proxy.addUser(expected);
        // Then
        assertThat(addedUser).isEqualTo(expected);

        // When
        User user = proxy.getUser(expected.getUserId());
        // Then
        assertThat(user).isEqualTo(expected);

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertThat(debugMessages).isNotEmpty();
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Added user")
        ));
    }
}