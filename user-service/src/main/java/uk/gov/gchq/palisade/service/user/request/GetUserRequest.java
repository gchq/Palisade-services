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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonCreator;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A {@code GetUserRequest} is a {@link Request} that is passed to the user-service
 * to get an existing {@link uk.gov.gchq.palisade.User}.
 * In order to get the user you must provide a {@link UserId} object to identify
 * the {@link uk.gov.gchq.palisade.User} you want.
 */
public class GetUserRequest extends Request {

    public final UserId userId;

    /**
     * @param userId the id of the {@link uk.gov.gchq.palisade.User} you want
     */
    @JsonCreator
    private GetUserRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("userId") final UserId userId) {
        setOriginalRequestId(originalRequestId);
        this.userId = requireNonNull(userId);
    }

    /**
     * Static factory method.
     *
     * @return {@link GetUserRequest}
     */
    public static GetUserRequest.IUserId create(final RequestId original) {
        return userId -> new GetUserRequest(null, original, userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetUserRequest)) return false;
        if (!super.equals(o)) return false;

        GetUserRequest that = (GetUserRequest) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(userId, that.userId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 61)
                .appendSuper(super.hashCode())
                .append(userId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GetUserRequest.class.getSimpleName() + "[", "]")
                .add("userId=" + userId)
                .toString();
    }

    public interface IUserId {
        /**
         * @param userId
         * @return the {@link GetUserRequest}
         */
        GetUserRequest withUserId(final UserId userId);
    }
}
