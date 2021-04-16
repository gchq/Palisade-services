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
package uk.gov.gchq.palisade.service.topicoffset.common.topicoffset;

import org.apache.kafka.common.header.Headers;

import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;

/**
 * Service contract for topic-offset-service.  Implementations are expected to return true when the condition are met
 * for sending a message with the offset setting.
 */
public interface TopicOffsetService {

    /**
     * Determines if this is an offset for the topic.
     *
     * @param headers map of headers
     * @return boolean if the conditions are met.
     */
    boolean isOffsetForTopic(Headers headers);


    /**
     * Given an offset, calls the TopicOffsetResponse builder to create a response message
     *
     * @param offset the offset of the start marker in the topic
     * @return the topic offset response
     */
    TopicOffsetResponse createTopicOffsetResponse(Long offset);
}

