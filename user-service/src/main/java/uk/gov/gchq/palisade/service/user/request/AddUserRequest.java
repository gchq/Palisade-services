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

package uk.gov.gchq.palisade.service.user.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.request.Request;
import uk.gov.gchq.palisade.service.user.service.UserService;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * An {@code AddUserRequest} is a {@link Request} that is passed to the {@link UserService}
 * to add a {@link User}.
 */
public class AddUserRequest extends Request {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddUserRequest.class);

    public final User user;

    @JsonCreator
    private AddUserRequest(@JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("user") final User user) {
        LOGGER.debug("AddUserRequest with originalRequestId {} and User {}", originalRequestId, user);
        setOriginalRequestId(originalRequestId);
        this.user = requireNonNull(user);
    }

    /**
     * Static factory method.
     *
     * @param original RequestId
     * @return {@link AddUserRequest}
     */
    public static IUser create(final RequestId original) {
        LOGGER.debug("AddUserRequest.create with requestId: {}", original);
        return user -> new AddUserRequest(original, user);
    }

    public interface IUser {
        /**
         * @param user {@link User}
         * @return the {@link AddUserRequest}
         */
        AddUserRequest withUser(final User user);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AddUserRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final AddUserRequest that = (AddUserRequest) o;
        return Objects.equals(user, that.user);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), user);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AddUserRequest.class.getSimpleName() + "[", "]")
                .add("user=" + user)
                .toString();
    }
}
