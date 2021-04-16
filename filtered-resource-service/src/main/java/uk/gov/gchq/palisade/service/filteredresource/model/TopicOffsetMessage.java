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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import uk.gov.gchq.palisade.service.filteredresource.common.Generated;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * TopicOffsetMessage represents the output of the filtered-resource-service which will be forwarded to the client provide the
 * information needed to retrieve the data for this Resource.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NONE)
public final class TopicOffsetMessage {

    /**
     * {@link String} reference to where the data is located
     */
    public final Long commitOffset; //pointer reference for the request given to the client

    @JsonCreator
    private TopicOffsetMessage(final @JsonProperty("commitOffset") Long commitOffset) {
        this.commitOffset = Optional.ofNullable(commitOffset).orElseThrow(() -> new IllegalArgumentException("Queue pointer cannot be null"));
    }

    /**
     * Builder class for the creation of instances of the TopicOffsetMessage.  This is a variant of the Fluent Builder
     * which will use Java Objects or JsonNodes equivalents for the components in the build.
     */
    public static class Builder {
        /**
         * Starter method for the Builder class.  This method is called to start the process of creating the
         * TopicOffsetMessage class.
         *
         * @return interface  {@link ICommitOffset} for the next step in the build.
         */
        public static ICommitOffset create() {
            return TopicOffsetMessage::new;
        }

        /**
         * Adds the queue pointer, a reference for the results for the request.
         */
        public interface ICommitOffset {

            /**
             * Adds the queue pointer to the message.
             *
             * @param commitOffset reference to the results for the request.
             * @return class {@link TopicOffsetMessage} for the completed class from the builder.
             */
            TopicOffsetMessage withCommitOffset(Long commitOffset);
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicOffsetMessage)) {
            return false;
        }
        TopicOffsetMessage that = (TopicOffsetMessage) o;
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
        return new StringJoiner(", ", TopicOffsetMessage.class.getSimpleName() + "[", "]")
                .add("commitOffset='" + commitOffset + "'")
                .add(super.toString())
                .toString();
    }
}
