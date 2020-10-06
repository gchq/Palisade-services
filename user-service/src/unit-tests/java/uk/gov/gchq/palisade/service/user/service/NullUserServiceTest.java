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

package uk.gov.gchq.palisade.service.user.service;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NullUserServiceTest {
    NullUserService nullUserService = new NullUserService();

    @Test
    public void testGetUser() {
        //Given the new user is not added to the service
        User user = new User().userId("testUser");

        //When we go to get the user
        Exception noSuchUserId = assertThrows(NoSuchUserIdException.class, () -> nullUserService.getUser(user.getUserId()), "No user found");

        //Then an error is thrown
        assertThat("No userId matching UserId[id='testUser'] found in cache").isEqualTo(noSuchUserId.getMessage());
    }

    @Test
    public void testAddUser() {
        User user = new User().userId("testUser");
        User actual = nullUserService.addUser(user);
        assertThat(user).isEqualTo(actual);
    }
}