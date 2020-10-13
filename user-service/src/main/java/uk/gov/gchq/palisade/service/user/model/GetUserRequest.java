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
package uk.gov.gchq.palisade.service.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A {@code GetUserRequest} is a {@link Request} that is passed to the user-service
 * to get an existing {@link User}.
 * In order to get the user you must provide a {@link UserId} object to identify
 * the {@link User} you want.
 */
public class GetUserRequest extends Request {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetUserRequest.class);
    public final UserId userId;

    /**
     * @param id                id
     * @param originalRequestId originalRequestId
     * @param userId            the id of the {@link User} you want
     */
    @JsonCreator
    private GetUserRequest(@JsonProperty("id") final RequestId id, @JsonProperty("originalRequestId") final RequestId originalRequestId, @JsonProperty("userId") final UserId userId) {
        LOGGER.debug("GetUserRequest with requestId: {}, originalRequestId {} and UserId {}", id, originalRequestId, userId);
        setOriginalRequestId(originalRequestId);
        this.userId = requireNonNull(userId);
    }

    /**
     * Static factory method.
     *
     * @param original requestId
     * @return {@link GetUserRequest}
     */
    public static GetUserRequest.IUserId create(final RequestId original) {
        LOGGER.debug("GetUserRequest.create with requestId: {}", original);
        return userId -> new GetUserRequest(null, original, userId);
    }

    public interface IUserId {
        /**
         * @param userId userId
         * @return the {@link GetUserRequest}
         */
        GetUserRequest withUserId(final UserId userId);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetUserRequest)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final GetUserRequest that = (GetUserRequest) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", GetUserRequest.class.getSimpleName() + "[", "]")
                .add("userId=" + userId)
                .toString();
    }
}
