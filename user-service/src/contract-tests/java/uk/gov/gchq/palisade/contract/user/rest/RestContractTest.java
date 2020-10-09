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

package uk.gov.gchq.palisade.contract.user.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.UserApplication;
import uk.gov.gchq.palisade.service.user.request.AddUserRequest;
import uk.gov.gchq.palisade.service.user.request.GetUserRequest;
import uk.gov.gchq.palisade.service.user.web.UserController;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("caffeine")
@SpringBootTest(classes = UserApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
class RestContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testContextLoads() {
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void testIsUp() {
        final ResponseEntity<String> health = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testAddedUserIsRetrievable() {
        // Given
        User user = new User().userId("rest-added-user").addAuths(Collections.singleton("authorisation")).addRoles(Collections.singleton("role"));

        // When
        AddUserRequest addUserRequest = AddUserRequest.create(new RequestId().id("addUserRequest")).withUser(user);
        Boolean addUserResponse = restTemplate.postForObject("/addUser", addUserRequest, Boolean.class);
        // Then
        assertThat(addUserResponse).isTrue();

        // When
        GetUserRequest getUserRequest = GetUserRequest.create(new RequestId().id("getUserRequest")).withUserId(user.getUserId());
        User getUserResponse = restTemplate.postForObject("/getUser", getUserRequest, User.class);
        // Then
        assertThat(getUserResponse).isEqualTo(user);
    }
}
