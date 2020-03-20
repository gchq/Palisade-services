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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import uk.gov.gchq.palisade.service.user.request.AddUserRequest;
import uk.gov.gchq.palisade.service.user.request.GetUserRequest;
import uk.gov.gchq.palisade.service.user.service.MockUserService;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotEquals;

@RunWith(JUnit4.class)
public class UserControllerTest {
    private UserController userController = new UserController(new MockUserService());

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(UserController.class);
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
    public void addAndGetUser() {
        User user = new User().userId("add-user-request-id").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));
        AddUserRequest addUserRequest = AddUserRequest.create(new RequestId().id("addUserRequest")).withUser(user);
        Boolean addedUser = userController.addUserRequest(addUserRequest);

        assertThat(true, is(equalTo(addedUser)));

        GetUserRequest getUserRequest = GetUserRequest.create(new RequestId().id("getUserRequest")).withUserId(user.getUserId());
        User expected = userController.getUserRequest(getUserRequest);
        assertThat(user, is(equalTo(expected)));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.INFO);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Invoking AddUserRequest:"),
                Matchers.anyOf(
                        Matchers.containsString("Invoking GetUserRequest: GetUserRequest"))
        ));
    }
}