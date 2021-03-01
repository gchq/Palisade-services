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
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenAuditErrorMessageEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A response from the token-AuditErrorMessage persistence system, providing the token and maybe
 * each an AuditErrorMessage or an exception. In proper usage, exactly one of the latter two
 * should be non-null and the other null - the token is always non-null.
 */
public final class TokenAuditErrorMessagePersistenceResponse {
    private final String token;
    private final List<TokenAuditErrorMessageEntity> messageEntities;
    private final Throwable exception;

    private TokenAuditErrorMessagePersistenceResponse(final String token, final List<TokenAuditErrorMessageEntity> messageEntities, final Throwable exception) {
        this.token = Optional.ofNullable(token).orElseThrow(() -> new IllegalArgumentException("token cannot be null"));
        this.messageEntities = Collections.unmodifiableList(messageEntities);
        this.exception = exception;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public List<TokenAuditErrorMessageEntity> getMessageEntities() {
        return messageEntities;
    }

    @Generated
    public Throwable getException() {
        return exception;
    }

    /**
     * Builder class for the creation of instances of the {@link TokenAuditErrorMessagePersistenceResponse}.
     * This is a variant of the Fluent Builder pattern.
     */
    public static class Builder {
        /**
         * Create a new fluent builder.
         *
         * @return the builder, expecting an {@link IToken} and then an {@link IAuditErrorMessageAndException}
         */
        public static IToken create() {
            return token -> (messageEntities, exception) -> new TokenAuditErrorMessagePersistenceResponse(token, messageEntities, exception);
        }

        /**
         * Supply a token to the builder
         */
        public interface IToken {
            /**
             * Supply a token to the builder
             *
             * @param token the client's token
             * @return the next step of the builder
             */
            IAuditErrorMessageAndException withToken(@NonNull String token);
        }

        /**
         * Supply either a token or an offset to the builder
         */
        public interface IAuditErrorMessageAndException {
            /**
             * Supply both an auditErrorMessage and exception to the builder.
             * In normal usage, one of these should be null and the other non-null.
             *
             * @param exception the exception thrown while finding the auditErrorMessage for the token, or null if none was thrown
             * @return a completed {@link TokenAuditErrorMessagePersistenceResponse} object
             * @implNote Prefer to call {@link IAuditErrorMessageAndException#withMessageEntities(List)} or {@link IAuditErrorMessageAndException#withException(Throwable)}
             * instead of this less-strict method.
             */
            TokenAuditErrorMessagePersistenceResponse withMessageEntitiesAndException(List<TokenAuditErrorMessageEntity> messageEntities, Throwable exception);

            /**
             * Supply just an auditErrorMessages to the builder.
             *
             * @return a completed {@link TokenAuditErrorMessagePersistenceResponse} object
             */
            default TokenAuditErrorMessagePersistenceResponse withMessageEntities(final @NonNull List<TokenAuditErrorMessageEntity> messageEntities) {
                return withMessageEntitiesAndException(messageEntities, null);
            }

            /**
             * Supply just an exception to the builder.
             *
             * @param exception the exception thrown while finding the offset for the token
             * @return a completed {@link TokenAuditErrorMessagePersistenceResponse} object
             */
            default TokenAuditErrorMessagePersistenceResponse withException(final @NonNull Throwable exception) {
                return withMessageEntitiesAndException(null, exception);
            }
        }
    }
}
