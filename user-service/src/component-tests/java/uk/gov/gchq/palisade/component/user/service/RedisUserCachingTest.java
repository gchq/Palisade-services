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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.contract.user.kafka.KafkaTestConfiguration;
import uk.gov.gchq.palisade.service.user.UserApplication;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.service.UserServiceProxy;
import uk.gov.gchq.palisade.service.user.stream.config.AkkaComponentsConfig;
import uk.gov.gchq.palisade.service.user.stream.config.AkkaRunnableGraph;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles({"redis", "akkatest"})
@SpringBootTest(classes = {UserApplication.class, RedisTestConfiguration.class, KafkaTestConfiguration.class}, webEnvironment = WebEnvironment.NONE)
class RedisUserCachingTest {

    @Autowired
    private UserServiceProxy userService;

    @Test
    void testAddedUserIsRetrievable() {
        // Given
        User user = new User().userId("added-user").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));

        // When
        User addedUser = userService.addUser(user);
        // Then
        assertThat(addedUser).isEqualTo(user);

        // When
        User getUser = userService.getUser(user.getUserId());
        // Then
        assertThat(getUser).isEqualTo(user);
    }

    @Test
    void testNonExistentUserRetrieveFails() {
        // Given
        UserId userId = new UserId().id("definitely-not-a-real-user");

        // When
        Exception noSuchUserId = assertThrows(NoSuchUserIdException.class, () -> userService.getUser(userId), "testMaxSizeTest should throw noSuchIdException");

        // Then - it is no longer found, it has been evicted
        // ie. throw NoSuchUserIdException
        assertThat("No userId matching UserId[id='definitely-not-a-real-user'] found in cache").isEqualTo(noSuchUserId.getMessage());
    }

    @Test
    void testUpdateUser() {
        // Given
        User user = new User().userId("updatable-user").addAuths(Collections.singleton("auth")).addRoles(Collections.singleton("role"));
        User update = new User().userId("updatable-user").addAuths(Collections.singleton("newAuth")).addRoles(Collections.singleton("newRole"));

        // When
        userService.addUser(user);
        userService.addUser(update);

        User updatedUser = userService.getUser(user.getUserId());

        // Then
        assertThat(updatedUser).isEqualTo(update);
    }

    @Test
    void testTtl() throws InterruptedException {
        // Given - a user was added a long time ago (ttl set to 1s in application.yaml)
        User user = new User().userId("ttl-test-user").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));
        userService.addUser(user);

        TimeUnit.MILLISECONDS.sleep(1000);

        // When - we try to access stale cache data
        Exception noSuchUserId = assertThrows(NoSuchUserIdException.class, () -> userService.getUser(user.getUserId()), "testMaxSizeTest should throw noSuchIdException");

        // Then - it is no longer found, it has been evicted
        // ie. throw NoSuchUserIdException
        assertThat("No userId matching UserId[id='ttl-test-user'] found in cache").isEqualTo(noSuchUserId.getMessage());
    }
}
