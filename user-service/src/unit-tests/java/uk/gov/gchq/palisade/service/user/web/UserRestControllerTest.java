/*
 * Copyright 2018-2021 Crown Copyright
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.UserService;
import uk.gov.gchq.palisade.service.user.service.UserServiceCachingProxy;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UserRestControllerTest {
    private final UserService service = Mockito.mock(UserService.class);
    private final UserServiceCachingProxy cacheProxy = new UserServiceCachingProxy(service);

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(UserServiceCachingProxy.class);
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
        var expected = new User().userId("add-user-request-id")
                .addAuths(Collections.singleton("authorisation"))
                .addRoles(Collections.singleton("role"));
        var request = UserRequest.Builder.create()
                .withUserId(expected.getUserId().getId())
                .withResourceId("test/resource")
                .withContext(new Context().purpose("purpose"));
        when(service.addUser(any(User.class))).thenReturn(expected);
        when(service.getUser(any(String.class))).thenReturn(expected);

        // When
        User addedUser = cacheProxy.addUser(expected);
        // Then
        assertThat(addedUser).isEqualTo(expected);

        // When
        User user = cacheProxy.getUser(request.getUserId());
        // Then
        assertThat(user)
                .as("Check that the User is the one we expect and has not been modified")
                .usingRecursiveComparison()
                .isEqualTo(expected);

        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.INFO);
        assertThat(infoMessages)
                .as("Check there are logging messages at INFO level")
                .isNotEmpty();
        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString("Cache add for userId add-user-request-id")
        ));
    }
}