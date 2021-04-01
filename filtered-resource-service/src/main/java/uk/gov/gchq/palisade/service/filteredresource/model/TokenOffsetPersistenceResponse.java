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

import java.util.Optional;

/**
 * A response from the token-offset persistence system, providing the token and maybe
 * each of the offset or an exception. In proper usage, exactly one of the latter two
 * should be non-null and the other null - the token is always non-null.
 */
public final class TokenOffsetPersistenceResponse {
    private final String token;
    private final Long offset;
    private final Throwable exception;

    private TokenOffsetPersistenceResponse(final String token, final Long offset, final Throwable exception) {
        this.token = Optional.ofNullable(token).orElseThrow(() -> new IllegalArgumentException("token cannot be null"));
        this.offset = offset;
        this.exception = exception;
    }

    @Generated
    public String getToken() {
        return token;
    }

    @Generated
    public Long getOffset() {
        return offset;
    }

    @Generated
    public Throwable getException() {
        return exception;
    }

    /**
     * Builder class for the creation of instances of the {@link TokenOffsetPersistenceResponse}.
     * This is a variant of the Fluent Builder pattern.
     */
    public static class Builder {
        /**
         * Create a new fluent builder.
         *
         * @return the builder, expecting an {@link IToken} and then an {@link IOffsetAndException}
         */
        public static IToken create() {
            return token -> (offset, exception) -> new TokenOffsetPersistenceResponse(token, offset, exception);
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
            IOffsetAndException withToken(@NonNull String token);
        }

        /**
         * Supply either a token or an offset to the builder
         */
        public interface IOffsetAndException {
            /**
             * Supply both an offset and exception to the builder.
             * In normal usage, one of these should be null and the other non-null.
             *
             * @param offset    the offset found for the client's token, or null if an exception was thrown
             * @param exception the exception thrown while finding the offset for the token, or null if none was thrown
             * @return a completed {@link TokenOffsetPersistenceResponse} object
             * @implNote Prefer to call {@link IOffsetAndException#withOffset(Long)} or {@link IOffsetAndException#withException(Throwable)}
             * instead of this less-strict method.
             */
            TokenOffsetPersistenceResponse withOffsetAndException(Long offset, Throwable exception);

            /**
             * Supply just an offset to the builder.
             *
             * @param offset the offset found for the client's token
             * @return a completed {@link TokenOffsetPersistenceResponse} object
             */
            default TokenOffsetPersistenceResponse withOffset(final @NonNull Long offset) {
                return withOffsetAndException(offset, null);
            }

            /**
             * Supply just an exception to the builder.
             *
             * @param exception the exception thrown while finding the offset for the token
             * @return a completed {@link TokenOffsetPersistenceResponse} object
             */
            default TokenOffsetPersistenceResponse withException(final @NonNull Throwable exception) {
                return withOffsetAndException(null, exception);
            }
        }
    }
}
