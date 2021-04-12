/*
 * Copyright 2018-2021 Crown Copyright
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
package uk.gov.gchq.palisade.service.data.model;

import org.springframework.lang.Nullable;

import uk.gov.gchq.palisade.service.data.common.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The class contains the audit message from the processing of the request. This will be a {@link AuditSuccessMessage}
 * or possibly an {@link AuditErrorMessage} message generated in the processing of the request.
 */
public final class AuditableDataResponse {

    private final String token;
    @Nullable
    private final AuditSuccessMessage auditSuccessMessage;
    @Nullable
    private final AuditErrorMessage auditErrorMessage;


    private AuditableDataResponse(
            final String token,
            @Nullable final AuditSuccessMessage auditSuccessMessage,
            @Nullable final AuditErrorMessage auditErrorMessage) {

        this.token = Optional.ofNullable(token).orElseThrow(() -> new IllegalArgumentException("token cannot be null"));
        this.auditSuccessMessage = auditSuccessMessage;
        this.auditErrorMessage = auditErrorMessage;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Nullable
    @Generated
    public AuditSuccessMessage getAuditSuccessMessage() {
        return auditSuccessMessage;
    }

    @Nullable
    @Generated
    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }

    /**
     * The static builder.
     */
    public static class Builder {

        /**
         * The starter method for the Builder class.
         *
         * @return the composed immutable object
         */
        public static IToken create() {
            return token -> success -> error -> new AuditableDataResponse(token, success, error);
        }

        /**
         * Adds the token to the response.
         */
        public interface IToken {

            /**
             * Adds the token to the response.
             *
             * @param token the client's unique token
             * @return interface {@link ISuccess} for the next step in the build.
             */
            ISuccess withToken(String token);
        }

        /**
         * Adds the auditSuccessMessage for the response.
         */
        public interface ISuccess {

            /**
             * Adds the AuditSuccessMessage for the response.
             *
             * @param auditSuccessMessage success message for the request.
             * @return interface {@link IError} for the next step in the build.
             */
            IError withSuccessMessage(AuditSuccessMessage auditSuccessMessage);
        }

        /**
         * Adds the attributes for the response.
         */
        public interface IError {

            /**
             * Adds the AuditErrorMessage to the response.
             *
             * @param auditErrorMessage error message for the request.
             * @return class {@link AuditableDataResponse} for the final step in the build.
             */
            AuditableDataResponse withAuditErrorMessage(AuditErrorMessage auditErrorMessage);

            /**
             * @return class {@link AuditableDataResponse} that does not have an {@link AuditErrorMessage}
             */
            default AuditableDataResponse withoutAuditErrorMessage() {
                return withAuditErrorMessage(null);
            }
        }

    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditableDataResponse)) {
            return false;
        }
        AuditableDataResponse that = (AuditableDataResponse) o;
        return Objects.equals(auditSuccessMessage, that.auditSuccessMessage) &&
                Objects.equals(auditErrorMessage, that.auditErrorMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(auditSuccessMessage, auditErrorMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", AuditableDataResponse.class.getSimpleName() + "[", "]")
                .add("auditSuccessMessage=" + auditSuccessMessage)
                .add("auditErrorMessage=" + auditErrorMessage)
                .toString();
    }
}
