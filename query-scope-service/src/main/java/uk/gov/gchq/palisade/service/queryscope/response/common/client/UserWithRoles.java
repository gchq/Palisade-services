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
package uk.gov.gchq.palisade.service.queryscope.response.common.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.queryscope.response.common.domain.IUser;
import uk.gov.gchq.palisade.service.queryscope.response.common.domain.User;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * User including the role that have for the request.  The set of possible roles include: User; Developer; and Administrator.
 */
public final class UserWithRoles implements IUser {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserWithRoles.class);
    private static final List<String> ALLOWED = Stream.of("USER", "DEV", "ADMIN").collect(toList());
    private static final String ROLE_KEY = "ROLES";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    @JsonProperty("userWithRoles")
    private final User userAndRoles; //named userAndRoles to avoid ambiguity with userWithRoles

    /**
     * user ID for the client.
     */
    @JsonIgnore
    public final String userId;

    /**
     * list of roles associated with this user.
     */
    @JsonIgnore
    public final List<String> roles;

    /**
     * This {@link Proxy} Class grants local access to protected elements within the {@link User} instance
     */
    private static class Proxy extends User {
        Proxy(final String userId, final Map<String, String> attributes) {
            super(userId, attributes);
        }

        Proxy(final User user) {
            super(user);
        }

        Map<String, String> getAttributes() {
            return super.attributes;
        }
    }

    @JsonCreator
    private UserWithRoles(@JsonProperty("userWithRoles") final User userWithRoles) {
        this.userAndRoles = userWithRoles;
        this.userId = userWithRoles.userId;
        this.roles = Collections.unmodifiableList(roleGen(new Proxy(userWithRoles).getAttributes()));
    }

    private UserWithRoles(final String userId, final Map<String, String> attributes) {
        this.userAndRoles = new Proxy(userId, attributes);
        this.userId = userId;
        this.roles = Collections.unmodifiableList(roleGen(attributes));
    }

    /**
     * Provides the public view of User without the associated rules.
     *
     * @return the UserWithRoles as a User
     */
    public User userWithRoles() {
        return this.userAndRoles;
    }


    private static List<String> roleGen(final Map<String, String> attributes) {
        return Optional.ofNullable(attributes.get(ROLE_KEY)).map((String val) -> {
            try {
                return MAPPER.readValue(val, MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                LOGGER.info("Failed to parse list of roles", e);
                return List.<String>of();
            }
        }).orElseGet(ArrayList::new);
    }

    /**
     * Starter method for the Builder class.  This method is called to start the process of creating the
     * UserWithRoles class.
     *
     * @param userId the given ID for the user.
     * @return interface  {@link IRoles} for the next step in the build.
     */
    public static IRoles create(final String userId) {
        return roles -> new UserWithRoles(userId, Stream.of(new AbstractMap.SimpleImmutableEntry<String, String>(ROLE_KEY,
                MAPPER.writeValueAsString(Stream.of(roles)
                        .peek(role -> Optional.of(ALLOWED.contains(role)).filter(val -> val).orElseThrow(() -> new RuntimeException("Invalid Role supplied")))
                        .collect(toList()))
        )).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)));
    }

    /**
     * Creates a UserWithRoles from a User.
     *
     * @param user that is being used to create the UserWithRoles.
     * @return new UserWithRoles.
     */
    public static UserWithRoles create(final User user) {
        return new UserWithRoles(user);
    }

    /**
     * Adds the roles to the UserWithRoles being build.
     */
    public interface IRoles {
        /**
         * Adds an array of roles.
         *
         * @param roles that are to be associated with this user.
         * @return the completed UserWithRoles instance.
         * @throws JsonProcessingException if it fails to parse the text
         */
        UserWithRoles withRoles(String... roles) throws JsonProcessingException;
    }
}



