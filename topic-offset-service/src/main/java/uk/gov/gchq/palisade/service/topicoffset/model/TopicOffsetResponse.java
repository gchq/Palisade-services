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
package uk.gov.gchq.palisade.service.topicoffset.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.gchq.palisade.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The TopicOffsetResponse is the output for Topic-Offset Service. It will only be created if the request message was
 * found to be the start marker for the messages of a specific client request. It will contain the offset in the message
 * queue for this first message. This is forwarded to the masked-resource-offset Kafka topic where it is used to prepare for
 * the client request to retrieve the data.
 * Note there is another class that effectively represent the same data but represent a different stage of the process.
 * <ul>
 * <li> {@code uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage} is the input for this the Filtered-Resource Service.
 * <li> {@link TopicOffsetResponse} is the output from the Topic-Offset Service.
 * </ul>
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class TopicOffsetResponse {

    private final Long commitOffset;  // Kafka commit offset of a start-of-stream message

    @JsonCreator
    private TopicOffsetResponse(
            final @JsonProperty("commitOffset") Long commitOffset) {

        this.commitOffset = Optional.ofNullable(commitOffset).orElseThrow(()
                -> new IllegalArgumentException("Commit offset cannot be null"));
    }

    @Generated
    public Long getCommitOffset() {
        return commitOffset;
    }

    /**
     * Builder class for the creation of instances of the TopicOffsetResponse. This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class. This method is called to start the process of creating the
         * TopicOffsetResponse class.
         *
         * @return interface to create a new {@link TopicOffsetResponse}.
         */
        public static CommitOffset create() {
            return TopicOffsetResponse::new;
        }

        /**
         * Adds the offset information to the message.
         */
        public interface CommitOffset {
            /**
             * Adds the offset for the message.
             *
             * @param commitOffset commit offset for the message.
             * @return interface {@link TopicOffsetResponse} for the completed class from the builder.
             */
            TopicOffsetResponse withOffset(Long commitOffset);
        }
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
