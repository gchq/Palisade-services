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
package uk.gov.gchq.palisade.service.topicoffset.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class TopicOffsetResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Long commitOffset;  //Kafka commit offset of a start-of-stream marker

    @JsonCreator
    private TopicOffsetResponse(
            final @JsonProperty("commitOffset") Long commitOffset) {

        this.commitOffset = Optional.ofNullable(commitOffset).orElseThrow(() -> new RuntimeException("Commit offset cannot be null"));
    }

    @Generated
    public Long getCommitOffset() {
        return commitOffset;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicOffsetResponse)) {
            return false;
        }
        TopicOffsetResponse that = (TopicOffsetResponse) o;
        return commitOffset.equals(that.commitOffset);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(commitOffset);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", TopicOffsetResponse.class.getSimpleName() + "[", "]")
                .add("commitOffset=" + commitOffset)
                .add(super.toString())
                .toString();
    }
}
