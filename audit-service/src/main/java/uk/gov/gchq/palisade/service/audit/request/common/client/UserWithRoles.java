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
package uk.gov.gchq.palisade.service.audit.request.common.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.service.audit.request.common.domain.IUser;
import uk.gov.gchq.palisade.service.audit.request.common.domain.User;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class UserWithRoles implements IUser {

    private static final List<String> ALLOWED = Stream.of("USER", "DEV", "ADMIN").collect(toList());
    private static final String ROLE_KEY = "ROLES";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    @JsonProperty("userWithRoles")
    private final User userWithRoles;

    @JsonIgnore
    public final String userId;
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
        this.userWithRoles = userWithRoles;
        this.userId = userWithRoles.userId;
        this.roles = Collections.unmodifiableList(roleGen(new Proxy(userWithRoles).getAttributes()));
    }

    private UserWithRoles(final String userId, final Map<String, String> attributes) {
        this.userWithRoles = new Proxy(userId, attributes);
        this.userId = userId;
        this.roles = Collections.unmodifiableList(roleGen(attributes));
    }


    public User userWithRoles() {
        return this.userWithRoles;
    }


    private static List<String> roleGen(final Map<String, String> attributes) {
        return Optional.ofNullable(attributes.get(ROLE_KEY)).map(val -> {
            try {
                return MAPPER.readValue(val, MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                return new ArrayList<String>();
            } catch (IOException e) {
                return new ArrayList<String>();
            }
        }).orElseGet(ArrayList::new);
    }


    public static IRoles create(final String userId) {
        return roles -> new UserWithRoles(userId, Stream.of(new AbstractMap.SimpleImmutableEntry<>(ROLE_KEY,
                MAPPER.writeValueAsString(Stream.of(roles)
                        .peek(role -> Optional.of(ALLOWED.contains(role)).filter(val -> val).orElseThrow(() -> new RuntimeException("Invalid Role supplied")))
                        .collect(toList()))
        )).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)));
    }


    public static UserWithRoles create(final User user) {
        return new UserWithRoles(user);
    }

    public interface IRoles {
        UserWithRoles withRoles(String... roles) throws JsonProcessingException;
    }
}