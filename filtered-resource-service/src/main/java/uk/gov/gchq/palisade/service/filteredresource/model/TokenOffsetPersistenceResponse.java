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

package uk.gov.gchq.palisade.service.filteredresource.model;

/**
 *
 */
public class TokenOffsetPersistenceResponse {
    public final String token;
    public final Long offset;
    public final Throwable exception;

    private TokenOffsetPersistenceResponse(final String token, final Long offset, final Throwable exception) {
        this.token = token;
        this.offset = offset;
        this.exception = exception;
    }

    public static class Builder {
        public static IToken create() {
            return token -> (offset, exception) -> new TokenOffsetPersistenceResponse(token, offset, exception);
        }

        public interface IToken {
            IOffsetAndException withToken(String token);
        }

        public interface IOffsetAndException {
            TokenOffsetPersistenceResponse withOffsetAndException(Long offset, Throwable exception);

            default TokenOffsetPersistenceResponse withOffset(Long offset) {
                return withOffsetAndException(offset, null);
            }

            default TokenOffsetPersistenceResponse withException(Throwable exception) {
                return withOffsetAndException(null, exception);
            }
        }
    }
}
