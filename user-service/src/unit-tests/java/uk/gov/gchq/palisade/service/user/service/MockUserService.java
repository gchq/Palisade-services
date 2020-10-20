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

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.user.exception.NoSuchUserIdException;
import uk.gov.gchq.palisade.service.user.model.UserRequest;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MockUserService extends HashMap<UserId, User> implements UserService {

    @Override
    public CompletableFuture<User> getUser(final UserRequest userRequest) {
        User user = this.get(userRequest.userId);
        if (Objects.nonNull(user)) {
            return CompletableFuture.completedFuture(user);
        } else {
            throw new NoSuchUserIdException("No such key: " + userRequest.getUserId());
        }
    }

    @Override
    public User addUser(final User user) {
        this.put(user.getUserId(), user);
        return user;
    }
}
