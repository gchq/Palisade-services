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

import uk.gov.gchq.palisade.service.filteredresource.common.Generated;
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenErrorMessageEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A response from the tokenErrorMessage persistence system, providing the token and maybe
 * each a {@link List} of {@link TokenErrorMessageEntity}(s) or an exception if there was an issue in persisting.
 * In proper usage, exactly one of the latter two should be non-null and the other null - the token is always non-null.
 */
public final class TokenErrorMessagePersistenceResponse {
    private final String token;
    private final List<TokenErrorMessageEntity> messageEntities;
    private final Throwable exception;

    private TokenErrorMessagePersistenceResponse(final String token, final List<TokenErrorMessageEntity> messageEntities, final Throwable exception) {
        this.token = Optional.ofNullable(token).orElseThrow(() -> new IllegalArgumentException("token cannot be null"));
        this.messageEntities = Collections.unmodifiableList(messageEntities);
        this.exception = exception;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public List<TokenErrorMessageEntity> getMessageEntities() {
        return messageEntities;
    }

    @Generated
    public Throwable getException() {
        return exception;
    }

    /**
     * Builder class for the creation of instances of the {@link TokenErrorMessagePersistenceResponse}.
     * This is a variant of the Fluent Builder pattern.
     */
    public static class Builder {
        /**
         * Create a new fluent builder.
         *
         * @return the builder, expecting an {@link IToken} and then either a {@link IMessageEntityAndException} or {@link Throwable} exception
         */
        public static IToken create() {
            return token -> (messageEntities, exception) -> new TokenErrorMessagePersistenceResponse(token, messageEntities, exception);
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
            IMessageEntityAndException withToken(@NonNull String token);
        }

        /**
         * Supply either a {@link List} of {@link TokenErrorMessageEntity}(s) or an exception to the builder
         */
        public interface IMessageEntityAndException {
            /**
             * Supply both a {@link List} of {@link TokenErrorMessageEntity}(s) and an exception to the builder.
             * In normal usage, one of these should be null and the other non-null.
             *
             * @param messageEntities a list of token and AuditErrorMessages that were saved and retrieved from persistence
             * @param exception       the exception thrown while finding the messageEntity for the token, or null if none was thrown
             * @return a completed {@link TokenErrorMessagePersistenceResponse} object
             * @implNote Prefer to call {@link IMessageEntityAndException#withMessageEntities(List)} or {@link IMessageEntityAndException#withException(Throwable)}
             * instead of this less-strict method.
             */
            TokenErrorMessagePersistenceResponse withMessageEntitiesAndException(List<TokenErrorMessageEntity> messageEntities, Throwable exception);

            /**
             * Supply just a {@link List} of {@link TokenErrorMessageEntity}(s) to the builder.
             *
             * @param messageEntities a List of entities that were saved and retrieved from persistence for the token requested
             * @return a completed {@link TokenErrorMessagePersistenceResponse} object
             */
            default TokenErrorMessagePersistenceResponse withMessageEntities(final @NonNull List<TokenErrorMessageEntity> messageEntities) {
                return withMessageEntitiesAndException(messageEntities, null);
            }

            /**
             * Supply just a {@link Throwable} exception to the builder.
             *
             * @param exception the exception thrown while finding the error message for the token
             * @return a completed {@link TokenErrorMessagePersistenceResponse} object
             */
            default TokenErrorMessagePersistenceResponse withException(final @NonNull Throwable exception) {
                return withMessageEntitiesAndException(null, exception);
            }
        }
    }
}
