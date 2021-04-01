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
package uk.gov.gchq.palisade.component.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.common.UserId;
import uk.gov.gchq.palisade.service.user.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.service.UserServiceCachingProxy;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        classes = {ApplicationConfiguration.class, CacheAutoConfiguration.class},
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.cache.caffeine.spec=expireAfterWrite=1s, maximumSize=100"}
)
@EnableCaching
@ActiveProfiles({"caffeine"})
class CaffeineUserCachingTest {

    @Autowired
    private UserServiceCachingProxy userService;

    @Autowired
    private CacheManager cacheManager;

    private void forceCleanUp() {
        ((Cache<?, ?>) Objects.requireNonNull(cacheManager.getCache("users")).getNativeCache()).cleanUp();
    }

    @Test
    void testAddedUserIsRetrievable() {
        // Given
        User user = new User().userId("added-user")
                .addAuths(Collections.singleton("authorisation"))
                .addRoles(Collections.singleton("role"));

        // When the user is added to the cache
        userService.addUser(user);
        // Then retrieve the user from the cache
        var addedUser = userService.getUser(user.getUserId().getId());
        assertThat(addedUser)
                .as("Check that the retrieved user is the same as the user we created")
                .usingRecursiveComparison()
                .isEqualTo(user);
    }

    @Test
    void testNonExistentUserRetrieveFails() {
        // Given the user is not added to the cache

        // When
        var noSuchUserIdException = assertThrows(NoSuchUserIdException.class,
                () -> userService.getUser("definitely-not-a-real-user"), "testNonExistentUser should throw noSuchIdException");

        // Then check the returned exception
        assertThat(noSuchUserIdException)
                .as("Check that the correct message is added to the Exception")
                .extracting("Message")
                .isEqualTo("No userId matching definitely-not-a-real-user found in cache");
    }

    @Test
    void testUpdateUser() {
        // Given we create an original user, and then update the users auths and roles
        User originalUser = new User().userId("updatable-user")
                .addAuths(Collections.singleton("auth"))
                .addRoles(Collections.singleton("role"));

        User updatedUser = new User().userId(originalUser.getUserId())
                .addAuths(Collections.singleton("newAuth"))
                .addRoles(Collections.singleton("newRole"));

        // When we add the original User
        userService.addUser(originalUser);
        // Then update the same User
        userService.addUser(updatedUser);

        // When we get the updated user
        User returnedUser = userService.getUser(updatedUser.getUserId().getId());

        // Then the User has been updated
        assertThat(returnedUser)
                .as("Check that the original user has been updated")
                .usingRecursiveComparison()
                .isEqualTo(updatedUser);
    }

    @Test
    void testMaxSize() {
        // Given - many users are added and cached (cache size set to 100 in application.yaml)
        Function<Integer, User> makeUser = i -> new User().userId(new UserId().id("max-size-" + i.toString() + "-test-user"));
        Function<Integer, String> makeUserId = i -> "max-size-" + i.toString() + "-test-user";
        for (int count = 0; count <= 150; ++count) {
            userService.addUser(makeUser.apply(count));
        }

        // When - we try to get the first (now-evicted) user to be added
        forceCleanUp();

        // Then a NoSuchUserIdException is thrown as the User no longer exists
        // When we get the user from the cache
        var noSuchUserIdException = assertThrows(NoSuchUserIdException.class,
                () -> userService.getUser(makeUserId.apply(0)), "testMaxSizeTest should throw noSuchIdException");

        // Then check the returned exception
        assertThat(noSuchUserIdException)
                .as("Check that the correct message is added to the Exception")
                .extracting("Message")
                .isEqualTo("No userId matching max-size-0-test-user found in cache");


    }

    @Test
    void testTtl() throws InterruptedException {
        // Given - a user was added a long time ago (ttl set to 1s in application.yaml)
        User user = new User().userId("ttl-test-user").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));
        userService.addUser(user);

        TimeUnit.SECONDS.sleep(1);

        // When - we try to access stale cache data
        forceCleanUp();

        // Then a NoSuchUserIdException is thrown as the User no longer exists
        assertThrows(NoSuchUserIdException.class,
                () -> userService.getUser(user.getUserId().getId()), "testMaxSizeTest should throw noSuchIdException"
        );
    }
}
