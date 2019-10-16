/*
 * Copyright 2019 Crown Copyright
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

package uk.gov.gchq.palisade.service.palisade.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.palisade.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.palisade.request.GetUserRequest;
import uk.gov.gchq.palisade.service.palisade.web.UserClient;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class UserServiceTest {

    private UserClient userClient = Mockito.mock(UserClient.class);
    private ApplicationConfiguration applicationConfig = new ApplicationConfiguration();
    private UserService userService;
    private UserId userId = new UserId().id("Bob");
    private User testUser;

    @Before
    public void setup() {
        userService = new UserService(userClient, applicationConfig.getAsyncExecutor());
        testUser = new User().userId(userId).roles("Role1", "Role2").auths("Auth1", "Auth2");
    }

    @Test
    public void getUserReturnsUser() {

        //Given
        when(userClient.getUser(any(GetUserRequest.class))).thenReturn(testUser);

        //When
        GetUserRequest request = new GetUserRequest().userId(userId);
        User actual = userService.getUser(request);

        //Then
        assertEquals(testUser, actual);
    }
}
