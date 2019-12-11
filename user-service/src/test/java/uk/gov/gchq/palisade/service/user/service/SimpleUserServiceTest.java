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
import org.junit.Test;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.repository.HashMapBackingStore;
import uk.gov.gchq.palisade.service.user.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.user.request.AddUserRequest;
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

    @Test
    public void shouldConfigureAndUseSharedData() {
        //Given
        User user = new User().userId("uid1").auths("test", "test2").roles("test_role");
        User user2 = new User().userId("uid2").auths("other_test").roles("role");
        CacheService cacheService = new SimpleCacheService().backingStore(new HashMapBackingStore(true));
        SimpleUserService hms = new SimpleUserService(cacheService);
        hms.addUser(AddUserRequest.create(new RequestId().id("new")).withUser(user)).join();

        //When
        SimpleUserService test = new SimpleUserService(cacheService);
        //add a user to the first service
        hms.addUser(AddUserRequest.create(new RequestId().id("new")).withUser(user2)).join();
        //both should be in the second service
        GetUserRequest getUserRequest1 = GetUserRequest.create(new RequestId().id("user1")).withUserId(new UserId().id("uid1"));
        GetUserRequest getUserRequest2 = GetUserRequest.create(new RequestId().id("user2")).withUserId(new UserId().id("uid2"));
        User actual1 = test.getUser(getUserRequest1).join();
        User actual2 = test.getUser(getUserRequest2).join();

        //Then
        assertThat(user, equalTo(actual1));
        assertThat(user2, equalTo(actual2));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Adding User"),
                Matchers.anyOf(
                        Matchers.containsString("User Added"))
        ));
    }

    @Test
    public void shouldSaveToCache() {
        //Given
        User user = new User().userId("uid1").auths("test", "test2").roles("test_role");
        CacheService cacheService = new SimpleCacheService().backingStore(new HashMapBackingStore(true));
        SimpleUserService hms = new SimpleUserService(cacheService);
        hms.addUser(AddUserRequest.create(new RequestId().id("new")).withUser(user)).join();

        //When
        SimpleUserService test = new SimpleUserService(cacheService);
        GetUserRequest getUserRequest = GetUserRequest.create(new RequestId().id("uid1")).withUserId(new UserId().id("uid1"));
        User actual1 = test.getUser(getUserRequest).join();

        //Then
        assertThat(actual1, equalTo(user));

        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.DEBUG);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("Adding User"),
                Matchers.anyOf(
                        Matchers.containsString("User Added"),
                        Matchers.containsString("found cached"))
        ));
    }

    @Test(expected = NoSuchUserIdException.class)
    public void throwOnNonExistentUser() throws Throwable {
        //Given
        CacheService cacheService = new SimpleCacheService().backingStore(new HashMapBackingStore(false));
        SimpleUserService hms = new SimpleUserService(cacheService);

        //When
        SimpleUserService test = new SimpleUserService(cacheService);
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