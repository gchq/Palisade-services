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

import java.util.Objects;
import java.util.Optional;

/**
 * This class is a container for {@code UserResponse} and {@code AuditErrorMessage} during stream processing.
 * Under normal conditions only one of these will be non-null, indicating failed or successful processing.
 */
public final class AuditableUserResponse {

    @JsonProperty("userResponse")
    private final UserResponse userResponse;
    @JsonProperty("auditErrorMessage")
    private final AuditErrorMessage auditErrorMessage;

    @JsonCreator
    private AuditableUserResponse(
            final @JsonProperty("userResponse") UserResponse userResponse,
            final @JsonProperty("auditErrorMessage") AuditErrorMessage auditErrorMessage) {
        this.userResponse = userResponse;
        this.auditErrorMessage = auditErrorMessage;
    }

    /**
     * Chain any errors from previous stream elements
     *
     * @param audit the previous audit or null
     * @return a new instance of this object
     */
    public AuditableUserResponse chain(final AuditErrorMessage audit) {
        return Optional.ofNullable(audit).map(message -> Builder.create()
                .withAuditErrorMessage(message))
                .orElse(this);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuditableUserResponse that = (AuditableUserResponse) o;
        return Objects.equals(userResponse, that.userResponse) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userResponse, auditErrorMessage);
    }

    public UserResponse getUserResponse() {
        return userResponse;
    }

    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }

    /**
     * The static builder
     */
    public static class Builder {

        /**
         * The creator function
         *
         * @return the composed immutable object
         */
        public static IUserResponse create() {
            return AuditableUserResponse::new;
        }

        /**
         * Compose with {@code UserResponse}
         */
        public interface IUserResponse {
            /**
             * Compose value
             *
             * @param audit value or null
             * @return value object
             */
            default AuditableUserResponse withAuditErrorMessage(AuditErrorMessage audit) {
                return withResponseAndError(null, audit);
            }


            /**
             * Compose value
             *
             * @param response value or null
             * @return value object
             */
            default AuditableUserResponse withUserResponse(UserResponse response) {
                return withResponseAndError(response, null);
            }

            /**
             * Compose value
             *
             * @param response          value or null
             * @param auditErrorMessage value or null
             * @return value object
             */
            AuditableUserResponse withResponseAndError(UserResponse response, AuditErrorMessage auditErrorMessage);
        }

    }

}
