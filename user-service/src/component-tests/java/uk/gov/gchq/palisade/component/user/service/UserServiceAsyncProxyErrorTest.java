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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.model.AuditableUserResponse;
import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.UserServiceAsyncProxy;

import java.util.concurrent.CompletableFuture;

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
class UserServiceAsyncProxyErrorTest {

    private static final Context CONTEXT = new Context().purpose("purpose");
    private static final UserRequest USER_REQUEST = UserRequest.Builder.create().withUserId("test-user-id").withResourceId("/test/resourceId").withContext(CONTEXT);
    private static final UserRequest NO_USER_REQUEST = UserRequest.Builder.create().withUserId("not-a-real-user").withResourceId("/test/resourceId").withContext(CONTEXT);

    @Autowired
    private UserServiceAsyncProxy userServiceAsyncProxy;
    @Autowired
    private ObjectMapper mapper;


    @Test
    void testContextLoads() {
        assertThat(userServiceAsyncProxy).isNotNull();
    }

    @Test
    void testGetUserSuccess() {
        // Given a user request
        final User user = new User().userId("test-user-id");
        // When adding to the cache
        this.userServiceAsyncProxy.addUser(user);

        // Then retrieving from the cache
        final CompletableFuture<AuditableUserResponse> subject = this.userServiceAsyncProxy.getUser(USER_REQUEST);

        // Check the CompletableFuture hasn't finished
        assertThat(subject.isDone()).isFalse();
        // Then complete the future
        AuditableUserResponse auditableUserResponse = subject.join();
        // Then Check it has been completed
        assertThat(subject.isDone()).isTrue();

        // Then the service suppresses exception and populates Audit object
        assertThat(auditableUserResponse.getAuditErrorMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isNull();

        // Then the cached User is the same as the original User
        assertThat(auditableUserResponse.getUserResponse().getUserId())
                .isEqualTo(user.getUserId().getId());
    }

    @Test
    void testGetUserFailure() {
        // When user has been added

        // Then retrieving a different user
        final CompletableFuture<AuditableUserResponse> subject = this.userServiceAsyncProxy.getUser(NO_USER_REQUEST);

        // Check the CompletableFuture hasn't finished
        assertThat(subject.isDone()).isFalse();
        // Then complete the future
        AuditableUserResponse auditableUserResponse = subject.join();
        // Then Check it has been completed
        assertThat(subject.isDone()).isTrue();

        // Then check that there is an error message
        assertThat(auditableUserResponse.getAuditErrorMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isNotNull();

        // Then check the error message contains the correct message
        assertThat(auditableUserResponse.getAuditErrorMessage().getError().getMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isEqualTo(NoSuchUserIdException.class.getName() + ": No userId matching not-a-real-user found in cache");
    }
}
