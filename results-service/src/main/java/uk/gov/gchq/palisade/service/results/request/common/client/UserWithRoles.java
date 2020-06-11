package uk.gov.gchq.palisade.service.results.request.common.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.gchq.palisade.service.policy.response.common.domain.IUser;
import uk.gov.gchq.palisade.service.policy.response.common.domain.User;

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

    private static final List<String> allowed = Stream.of("USER", "DEV", "ADMIN").collect(toList());
    private static final String ROLE_KEY = "ROLES";
    private static final ObjectMapper mapper = new ObjectMapper();


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
        public Proxy(final String userId, final Map<String, String> attributes) {
            super(userId, attributes);
        }

        public Proxy(final User user) {
            super(user);
        }

        public Map<String, String> getAttributes() {
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

    /**
     * Utility function
     */
    private static List<String> roleGen(final Map<String, String> attributes) {
        return Optional.ofNullable(attributes.get(ROLE_KEY)).map(val -> {
            try {
                return mapper.readValue(val, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                return new ArrayList<String>();
            } catch (IOException e) {
                return new ArrayList<String>();
            }
        }).orElseGet(ArrayList::new);
    }

    /**
     * Static factory method
     */
    public static IRoles create(final String userId) {
        return roles -> new UserWithRoles(userId, Stream.of(new AbstractMap.SimpleImmutableEntry<>(ROLE_KEY,
                mapper.writeValueAsString(Stream.of(roles)
                        .peek(role -> Optional.of(allowed.contains(role)).filter(val -> val).orElseThrow(() -> new RuntimeException("Invalid Role supplied")))
                        .collect(toList()))
        )).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)));
    }

    /**
     * Conversion utility method
     */
    public static UserWithRoles create(final User user) {
        return new UserWithRoles(user);
    }

    public interface IRoles {
        UserWithRoles withRoles(String... roles) throws JsonProcessingException;
    }
}


