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
package uk.gov.gchq.palisade.service.palisade.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Response message that is returned to the client.
 * This message contains a unique token to link the request from the client to the data, and is used at the Filtered Resource Service to retrieve data via the Data Service.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class PalisadeClientResponse {

    private final String token;  //unique identifier for the request.

    /**
     * Instantiates a new Response, taking in a unique request token as a parameter.
     *
     * @param token the unique request token
     */
    @JsonCreator
    public PalisadeClientResponse(final @JsonProperty("token") String token) {
        this.token = Optional.ofNullable(token).orElseThrow(() -> new IllegalArgumentException("token cannot be null"));
    }

    /**
     * Gets token.
     *
     * @return the unique token
     */
    @Generated
    public String getToken() {
        return token;
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", PalisadeClientResponse.class.getSimpleName() + "[", "]")
                .add("token='" + token + "'")
                .toString();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PalisadeClientResponse palisadeClientResponse = (PalisadeClientResponse) o;
        return Objects.equals(token, palisadeClientResponse.token);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(token);
    }
}
