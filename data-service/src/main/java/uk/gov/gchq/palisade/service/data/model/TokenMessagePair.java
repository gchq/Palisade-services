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

import uk.gov.gchq.palisade.service.data.common.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The token message pair used in the sending of audit messages to the Audit Service.
 */
public final class TokenMessagePair {


    private final String token; //unique identifier for the request

    private final AuditMessage auditMessage; //audit success or error message

    /**
     * Instantiates a new token and audit message pair.
     *
     * @param token        the token.
     * @param auditMessage the original request.
     */
    private TokenMessagePair(final String token, final AuditMessage auditMessage) {

        this.token = Optional.ofNullable(token).orElseThrow(() -> new IllegalArgumentException("token" + " cannot be null"));
        this.auditMessage = Optional.ofNullable(auditMessage).orElseThrow(() -> new IllegalArgumentException("auditMessage" + " cannot be null"));
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public AuditMessage getAuditMessage() {
        return auditMessage;
    }

    /**
     * Builder class for the creation of instances of the TokenMessagePair.  This uses a variant of the Fluent Builder
     * for the creation of an immutable instance of the object.
     */
    public static class Builder {

        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * TokenMessagePair instance.
         *
         * @return interface {@link IToken} for the next step in the build.
         */
        public static IToken create() {
            return token -> auditMessage -> new TokenMessagePair(token, auditMessage);
        }

        /**
         * Adds the token to the {@code TokenMessagePair}.
         */
        public interface IToken {
            /**
             * Adds the token.
             *
             * @param token token to uniquely identify the request.
             * @return interface {@link IAuditMessage} for the for the next step in the build.
             */
            IAuditMessage withToken(String token);
        }

        /**
         * Adds the audit message to the {@code TokenMessagePair}.
         */
        public interface IAuditMessage {
            /**
             * Adds the audit message.
             *
             * @param auditMessage audit message that is to be sent.
             * @return class {@link TokenMessagePair} for the final step in the build.
             */
            TokenMessagePair withAuditMessage(AuditMessage auditMessage);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenMessagePair)) {
            return false;
        }
        TokenMessagePair that = (TokenMessagePair) o;
        return token.equals(that.token) &&
                auditMessage.equals(that.auditMessage);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token, auditMessage);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", TokenMessagePair.class.getSimpleName() + "[", "]")
                .add("token=" + token)
                .add("auditMessage=" + auditMessage)
                .toString();
    }
}
