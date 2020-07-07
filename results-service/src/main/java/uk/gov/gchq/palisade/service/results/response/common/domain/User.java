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
package uk.gov.gchq.palisade.service.results.response.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Base class for User.  Provides the information about a user in the system.
 */
public class User implements IUser {

    /**
     * user ID for the client.
     */
    @JsonProperty("user_id")
    public final String userId;

    @JsonProperty("attributes")
    protected final Map<String, String> attributes; // must be protected


    private User(final String userId) {
        this.userId = userId;
        this.attributes = new HashMap<>();
    }

    /**
     * Copy constructor for a {@link User}.
     *
     * @param user the {@link User} that will be copied.
     */
    protected User(final User user) {
        requireNonNull(user, "User to be cloned cannot be null");
        this.userId = user.userId;
        this.attributes = user.attributes;


    }

    @JsonCreator
    protected User(@JsonProperty("user_id") final String userId, @JsonProperty("attributes") final Map<String, String> entries) {
        this.userId = userId;
        this.attributes = Collections.unmodifiableMap(entries);
    }

    /**
     * Builds an instance of the User class with the user ID.
     * @param userId for this user.
     * @return new instance of User.
     */
    public static User create(final String userId) {
        return new User(userId);
    }


}
