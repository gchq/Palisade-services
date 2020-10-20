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
package uk.gov.gchq.palisade.component.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.component.user.KafkaTestConfiguration;
import uk.gov.gchq.palisade.service.user.UserApplication;
import uk.gov.gchq.palisade.service.user.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.UserServiceProxy;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles({"caffeine", "akkatest"})
@SpringBootTest(
        classes = {UserApplication.class, ApplicationConfiguration.class, KafkaTestConfiguration.class},
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.cache.caffeine.spec=expireAfterWrite=1s, maximumSize=100"}
)
class CaffeineUserCachingTest {

    @Autowired
    private UserServiceProxy userService;

    @Autowired
    private CacheManager cacheManager;

    private void forceCleanUp() {
        ((Cache<?, ?>) Objects.requireNonNull(cacheManager.getCache("users")).getNativeCache()).cleanUp();
    }

    @Test
    void testAddedUserIsRetrievable() {
        // Given
        User user = new User().userId("added-user").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));
        UserRequest request = UserRequest.Builder.create().withUserId(user.getUserId().getId()).withResourceId("test/resource").withContext(new Context().purpose("test"));

        // When
        User addedUser = userService.addUser(user);
        // Then
        assertThat(addedUser).isEqualTo(user);

        // When
        User getUser = userService.getUser(request).join();
        // Then
        assertThat(getUser).isEqualTo(user);
    }

    @Test
    void testNonExistentUserRetrieveFails() {
        // Given
        UserId userId = new UserId().id("definitely-not-a-real-user");
        UserRequest request = UserRequest.Builder.create().withUserId(userId.getId()).withResourceId("test/resource").withContext(new Context().purpose("test"));

        // When
        Exception noSuchUserId = assertThrows(NoSuchUserIdException.class,
                () -> userService.getUser(request), "NonExistentUser should throw noSuchIdException"
        );

        //Then
        assertThat(noSuchUserId.getMessage()).isEqualTo("No userId matching UserId[id='definitely-not-a-real-user'] found in cache");
    }

    @Test
    void testUpdateUser() {
        // Given
        User user = new User().userId("updatable-user").addAuths(Collections.singleton("auth")).addRoles(Collections.singleton("role"));
        User update = new User().userId("updatable-user").addAuths(Collections.singleton("newAuth")).addRoles(Collections.singleton("newRole"));
        UserRequest userRequest = UserRequest.Builder.create().withUserId(user.getUserId().getId()).withResourceId("test/resource").withContext(new Context().purpose("test"));

        // When
        userService.addUser(user);
        userService.addUser(update);

        User updatedUser = userService.getUser(userRequest).join();

        // Then
        assertThat(updatedUser).isEqualTo(update);
    }

    @Test
    void testMaxSize() {
        // Given - many users are added and cached (cache size set to 100 in application.yaml)
        Function<Integer, User> makeUser = i -> new User().userId(new UserId().id("max-size-" + i.toString() + "-test-user"));
        Function<Integer, UserRequest> makeUserRequest = i -> UserRequest.Builder.create()
                .withUserId("max-size-" + i.toString() + "-test-user")
                .withResourceId("test/resource")
                .withContext(new Context().purpose("purpose"));
        for (int count = 0; count <= 150; ++count) {
            userService.addUser(makeUser.apply(count));
        }

        // When - we try to get the first (now-evicted) user to be added
        forceCleanUp();
        Exception noSuchUserId = assertThrows(NoSuchUserIdException.class,
                () -> userService.getUser(makeUserRequest.apply(0)), "testMaxSizeTest should throw noSuchIdException"
        );

        // Then - it is no longer found, it has been evicted
        // ie. throw NoSuchUserIdException
        assertThat(noSuchUserId.getMessage()).isEqualTo("No userId matching UserId[id='max-size-0-test-user'] found in cache");
    }

    @Test
    void testTtl() throws InterruptedException {
        // Given - a user was added a long time ago (ttl set to 1s in application.yaml)
        User user = new User().userId("ttl-test-user").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));
        userService.addUser(user);

        TimeUnit.MILLISECONDS.sleep(1000);

        // When - we try to access stale cache data
        forceCleanUp();
        Exception noSuchUserId = assertThrows(NoSuchUserIdException.class,
                () -> userService.getUser(UserRequest.Builder.create()
                        .withUserId(user.getUserId().getId())
                        .withResourceId("test/resource")
                        .withContext(new Context().purpose("purpose"))
                ), "testMaxSizeTest should throw noSuchIdException"
        );

        // Then - it is no longer found, it has been evicted
        // ie. throw NoSuchUserIdException
        assertThat(noSuchUserId.getMessage()).isEqualTo("No userId matching UserId[id='ttl-test-user'] found in cache");
    }
}
