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
package uk.gov.gchq.palisade.service.palisade.request;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.service.request.Request;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * A {@code GetUserRequest} is a {@link Request} that is passed to the user-service
 * to get an existing {@link User}.
 * In order to get the user you must provide a {@link UserId} object to identify
 * the {@link uk.gov.gchq.palisade.User} you want.
 */
public class GetUserRequest extends Request {

    private UserId userId;

    /**
     * Constructs a {@link GetUserRequest} without a {@link UserId}.
     */
    public GetUserRequest() {
        // no-args constructor needed for serialization only
    }

    /**
     * @param userId the id of the {@link uk.gov.gchq.palisade.User} you want
     * @return the {@link GetUserRequest}
     */
    @Generated
    public GetUserRequest userId(final UserId userId) {
        requireNonNull(userId, "The user id cannot be set to null.");
        this.setUserId(userId);
        return this;
    }

    @Generated
    public UserId getUserId() {
        return userId;
    }

    @Generated
    public void setUserId(final UserId userId) {
        requireNonNull(userId);
        this.userId = userId;
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
