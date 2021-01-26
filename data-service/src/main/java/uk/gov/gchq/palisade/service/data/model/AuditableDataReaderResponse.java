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

import uk.gov.gchq.palisade.Generated;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public class AuditableDataReaderResponse {

    private final String token;
    private final @Nullable AuditSuccessMessage auditSuccessMessage;
    private final @Nullable AuditErrorMessage auditErrorMessage;


    private AuditableDataReaderResponse(
            final String token,
            final @Nullable AuditSuccessMessage auditSuccessMessage,
            final @Nullable AuditErrorMessage auditErrorMessage) {

        this.token = Optional.ofNullable(token)
                .orElseThrow(() -> new IllegalArgumentException("token cannot be null"));
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
     * The static builder
     */
    public static class Builder {

        /**
         * The starter method for the Builder class.
         * *
         * @return the composed immutable object
         */
        public static IToken create() {
            return  token ->  success -> error ->  new AuditableDataReaderResponse(token, success, error);
        }

        /**
         * Adds the token to the message
         */
        public interface IToken {
            /**
             * Adds the token to the request
             *
             * @param token the client's unique token
             * @return interface {@link ISuccess} for the next step in the build.
             */
            ISuccess withToken(String token);
        }

        /**
         * Adds the auditSuccessMessage for the message.
         */
        public interface ISuccess {
            /**
             * Adds the su for the message.
             *
             * @param auditSuccessMessage timestamp for the request.
             * @return interface {@link IError} for the next step in the build.
             */
            IError withSuccessMessage(AuditSuccessMessage auditSuccessMessage);

        }

        /**
         * Adds the attributes for the message.
         */
        public interface IError {
            /**
             * Adds the attributes for the message.
             *
             * @param auditErrorMessage timestamp for the request.
             * @return interface {@link AuditableDataReaderResponse} for the final step in the build.
             */
            AuditableDataReaderResponse withAuditErrorMessage(AuditErrorMessage auditErrorMessage);
        }

    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditableDataReaderResponse)) {
            return false;
        }
        AuditableDataReaderResponse that = (AuditableDataReaderResponse) o;
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
        return new StringJoiner(", ", AuditableDataReaderResponse.class.getSimpleName() + "[", "]")
                .add("auditSuccessMessage=                " + auditSuccessMessage)
                .add("auditErrorMessage=                " + auditErrorMessage)
                .toString();
    }
}
