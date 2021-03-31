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

package uk.gov.gchq.palisade.contract.audit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.gchq.palisade.service.audit.common.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Specific test class for serialization testing
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BadRequest {

    private final String message;

    @JsonCreator
    private BadRequest(final @JsonProperty("message") String message) {
        this.message = Optional.ofNullable(message).orElseThrow(() -> new IllegalArgumentException("The message cannot be null"));
    }

    @Generated
    public String getMessage() {
        return message;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BadRequest)) {
            return false;
        }
        final BadRequest that = (BadRequest) o;
        return message.equals(that.message);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(message);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", BadRequest.class.getSimpleName() + "[", "]")
                .add("message='" + message + "'")
                .toString();
    }
}
