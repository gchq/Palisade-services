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

package uk.gov.gchq.palisade.service.filteredresource.model;

import org.springframework.lang.NonNull;

import uk.gov.gchq.palisade.Generated;

import java.util.Optional;

/**
 * A response from the token-exception persistence system, providing the token and maybe
 * each of the exception or a exception when processing. In proper usage, exactly one of the latter two
 * should be non-null and the other null - the token is always non-null.
 */
public final class TokenAuditErrorMessagePersistenceResponse {
    private final String token;
    private final AuditErrorMessage auditErrorMessage;
    private final Throwable processingException;

    private TokenAuditErrorMessagePersistenceResponse(final String token, final AuditErrorMessage auditErrorMessage, final Throwable processingException) {
        this.token = Optional.ofNullable(token).orElseThrow(() -> new IllegalArgumentException("token cannot be null"));
        this.auditErrorMessage = auditErrorMessage;
        this.processingException = processingException;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public AuditErrorMessage getAuditErrorMessage() {
        return auditErrorMessage;
    }

    @Generated
    public Throwable getProcessingException() {
        return processingException;
    }

    /**
     * Builder class for the creation of instances of the {@link TokenAuditErrorMessagePersistenceResponse}.
     * This is a variant of the Fluent Builder pattern.
     */
    public static class Builder {
        /**
         * Create a new fluent builder.
         *
         * @return the builder, expecting an {@link IToken} and then an {@link IAuditErrorMessageAndProcessingException}
         */
        public static IToken create() {
            return token -> (auditErrorMessage, processingException) -> new TokenAuditErrorMessagePersistenceResponse(token, auditErrorMessage, processingException);
        }

        /**
         * Supply a token to the builder
         */
        public interface IToken {
            /**
             * Supply a token to the builder
             *
             * @param token the unique request token
             * @return the next step of the builder
             */
            IAuditErrorMessageAndProcessingException withToken(@NonNull String token);
        }

        /**
         * Supply either a token or an exception to the builder
         */
        public interface IAuditErrorMessageAndProcessingException {
            /**
             * Supply both an exception and exception to the builder.
             * In normal usage, one of these should be null and the other non-null.
             *
             * @param auditErrorMessage   the {@link AuditErrorMessage} associated with this token
             * @param processingException the exception thrown while finding the auditErrorMessage for the token, or null if none was thrown
             * @return a completed {@link TokenAuditErrorMessagePersistenceResponse} object
             * @implNote Prefer to call {@link IAuditErrorMessageAndProcessingException#withAuditErrorMessage(AuditErrorMessage)} or
             * {@link IAuditErrorMessageAndProcessingException#withProcessingException(Throwable)} instead of this less-strict method.
             */
            TokenAuditErrorMessagePersistenceResponse withAuditErrorMessageAndProcessingException(AuditErrorMessage auditErrorMessage, Throwable processingException);

            /**
             * Supply just an AuditErrorMessage to the builder.
             *
             * @param auditErrorMessage the {@link AuditErrorMessage} associated with this token
             * @return a completed {@link TokenAuditErrorMessagePersistenceResponse} object
             */
            default TokenAuditErrorMessagePersistenceResponse withAuditErrorMessage(final @NonNull AuditErrorMessage auditErrorMessage) {
                return withAuditErrorMessageAndProcessingException(auditErrorMessage, null);
            }

            /**
             * Supply just an exception to the builder.
             *
             * @param processingException the exception thrown while finding the auditErrorMessage for the token
             * @return a completed {@link TokenAuditErrorMessagePersistenceResponse} object
             */
            default TokenAuditErrorMessagePersistenceResponse withProcessingException(final @NonNull Throwable processingException) {
                return withAuditErrorMessageAndProcessingException(null, processingException);
            }
        }
    }
}
