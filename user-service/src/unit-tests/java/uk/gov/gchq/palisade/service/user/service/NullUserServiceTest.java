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

package uk.gov.gchq.palisade.service.user.service;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.user.common.Context;
import uk.gov.gchq.palisade.service.user.common.User;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.model.UserRequest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NullUserServiceTest {
    final NullUserService nullUserService = new NullUserService();

    @Test
    void testGetUser() {
        // Given the new user is not added to the service
        User user = new User().userId("testUser");
        UserRequest request = UserRequest.Builder.create().withUserId(user.getUserId().getId()).withResourceId("test/resource").withContext(new Context().purpose("purpose"));

        // When we go to get the user
        Exception noSuchUserId = assertThrows(NoSuchUserIdException.class, () -> nullUserService.getUser(request.userId), "No user found");

        // Then an error is thrown
        assertThat(noSuchUserId.getMessage())
                .as("Check that the right message is attached to the Exception when the user hasn't been added to the cache")
                .isEqualTo("No userId matching testUser found in cache");
    }

    @Test
    void testAddUser() {
        // Given a new User is created
        User user = new User().userId("testUser")
                .addRoles(Collections.singleton("testRole"))
                .addAuths(Collections.singleton("testAuth"));

        // When we add the user to the Service, and the added user is returned
        User actual = nullUserService.addUser(user);

        // Then check the returned user has not been modified
        assertThat(user)
                .as("Check that the user returned is the correct user and the contents has not been modified")
                .usingRecursiveComparison()
                .isEqualTo(actual);
    }
}
