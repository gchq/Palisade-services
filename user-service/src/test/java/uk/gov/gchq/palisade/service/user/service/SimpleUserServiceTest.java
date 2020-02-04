/*
 * Copyright 2018 Crown Copyright
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

package uk.gov.gchq.palisade.service.user.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.request.GetUserRequest;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SimpleUserServiceTest {
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(SimpleUserService.class);
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

    @Ignore
    @Test(expected = NoSuchUserIdException.class)
    public void throwOnNonExistentUser() throws Throwable {
        //Given
        SimpleUserService hms = new SimpleUserService();

        //When
        SimpleUserService test = new SimpleUserService();
        try {
            GetUserRequest getUserRequest = GetUserRequest.create(new RequestId().id("uid1")).withUserId(new UserId().id("uid1"));
            User actual1 = test.getUser(getUserRequest).join();
        } catch (CompletionException e) {
            throw e.getCause();
        }
        //Then
        fail("exception expected");
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        List<String> errorMessages = getMessages(event -> event.getLevel() == Level.ERROR);
        assertNotEquals(0, debugMessages.size());
        assertThat(1, equalTo(errorMessages.size()));
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Getting User"),
                Matchers.anyOf(
                        Matchers.containsString("User not found"))
        ));
    }
}