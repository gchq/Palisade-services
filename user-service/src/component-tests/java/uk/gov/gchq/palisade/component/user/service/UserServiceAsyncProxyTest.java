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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.common.user.User;
import uk.gov.gchq.palisade.service.user.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.user.model.AuditableUserResponse;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.UserServiceAsyncProxy;

import java.util.Collections;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify the handling of exceptions, and the population of audit objects during stream processing
 */
@SpringBootTest(
        classes = {ApplicationConfiguration.class, CacheAutoConfiguration.class},
        webEnvironment = WebEnvironment.NONE,
        properties = {"spring.cache.caffeine.spec=expireAfterWrite=1s, maximumSize=100"}
)
@EnableCaching
@ActiveProfiles({"caffeine"})
class UserServiceAsyncProxyTest {

    private static final Context CONTEXT = new Context().purpose("purpose");
    private static final UserRequest USER_REQUEST = UserRequest.Builder.create().withUserId("test-user-id").withResourceId("/test/resourceId").withContext(CONTEXT);
    private static final UserRequest NO_USER_REQUEST = UserRequest.Builder.create().withUserId("not-a-real-user").withResourceId("/test/resourceId").withContext(CONTEXT);

    @Autowired
    private UserServiceAsyncProxy userServiceAsyncProxy;

    @Test
    void testContextLoads() {
        assertThat(userServiceAsyncProxy)
                .as("Check that the User Service has started successfully")
                .isNotNull();
    }

    @Test
    void testGetUserSuccess() {
        // Given a user
        var user = new User().userId("test-user-id")
                .addAuths(Collections.singleton("authorisation"))
                .addRoles(Collections.singleton("role"));

        // When adding to the cache
        this.userServiceAsyncProxy.addUser(user);

        // Then retrieving from the cache
        var userResponse = this.userServiceAsyncProxy.getUser(USER_REQUEST).join();

        // Then the service suppresses exception and populates Audit object
        assertThat(userResponse)
                .as("Check that no Error was added to the AuditableUserResponse")
                .extracting(AuditableUserResponse::getAuditErrorMessage)
                .isNull();

        // Then the cached User is the same as the original User
        assertThat(userResponse)
                .as("Check that the user in the response is the same as the one we added to the cache")
                .extracting(AuditableUserResponse::getUserResponse)
                .extracting("user")
                .isEqualTo(user);
    }

    @Test
    void testGetUserFailure() {
        // When no user has been added

        // Then retrieving a different user
        var auditableUserResponse = this.userServiceAsyncProxy.getUser(NO_USER_REQUEST).join();

        // Then check that there is an error message
        assertThat(auditableUserResponse)
                .as("verify that exception is propagated into an auditable object and returned")
                .extracting(AuditableUserResponse::getAuditErrorMessage)
                .extracting(AuditErrorMessage::getError)
                .isInstanceOf(CompletionException.class)
                .extracting("Message")
                .isEqualTo(NoSuchUserIdException.class.getName() + ": No userId matching not-a-real-user found in cache");

        assertThat(auditableUserResponse)
                .as("Check that the UserResponse is empty")
                .extracting(AuditableUserResponse::getUserResponse)
                .isNull();

    }
}
