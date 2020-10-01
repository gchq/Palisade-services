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

import org.junit.Test;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;

import static org.assertj.core.api.Assertions.assertThat;

public class NullUserServiceTest {
    NullUserService nullUserService = new NullUserService();

    @Test(expected = NoSuchUserIdException.class)
    public void getUser() {
        User user = new User().userId("testUser");
        nullUserService.getUser(user.getUserId());
    }

    @Test
    public void addUser() {
        User user = new User().userId("testUser");
        User actual = nullUserService.addUser(user);
        assertThat(user).isEqualTo(actual);
    }
}