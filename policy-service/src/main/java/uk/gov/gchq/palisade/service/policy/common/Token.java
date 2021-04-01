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

package uk.gov.gchq.palisade.service.policy.common;

/**
 * Simply stores the expected header key for Tokens
 * Since the content of tokens are strings, there is no need for further implementation
 * If desired, this could extend eg. UUID if more meaningful Token processing was desired
 */
public final class Token {
    public static final String HEADER = "x-request-token";

    private Token() {
        // Tokens are just strings, no need to instantiate a class for them
    }

    /**
     * Map a token to a partition, given the maximum number of partitions on the topic.
     * This ensures that all messages in a single request remain ordered, but allows for scaling
     * across separate requests.
     *
     * @param token      the token string
     * @param partitions the maximum number of partitions on the topic
     * @return a partition number for the token, between 0 (inclusive) and partitions (exclusive)
     */
    public static int toPartition(final String token, final int partitions) {
        return Math.floorMod(token.hashCode(), partitions);
    }

}
