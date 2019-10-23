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

package uk.gov.gchq.palisade.service.user.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.request.Request;
import uk.gov.gchq.palisade.service.user.service.UserService;

import static java.util.Objects.requireNonNull;

/**
 * An {@code AddUserRequest} is a {@link Request} that is passed to the {@link UserService}
 * to add a {@link User}.
 */
public class AddUserRequest extends Request {
    public final User user;

    @JsonCreator
    private AddUserRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("user") final User user) {
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
        return user -> new AddUserRequest(null, original, user);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AddUserRequest that = (AddUserRequest) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(user, that.user)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 61)
                .appendSuper(super.hashCode())
                .append(user)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("user", user)
                .toString();
    }

    public interface IUser {
        /**
         * @param user {@link User}
         * @return the {@link AddUserRequest}
         */
        AddUserRequest withUser(final User user);
    }
}

