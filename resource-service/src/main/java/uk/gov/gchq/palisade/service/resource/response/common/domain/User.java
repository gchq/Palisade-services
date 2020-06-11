package uk.gov.gchq.palisade.service.resource.response.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class User implements IUser {

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

    public static User create(final String userId) {
        return new User(userId);
    }


}
