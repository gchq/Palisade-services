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
package uk.gov.gchq.palisade.service.topicoffset.service;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.topicoffset.common.topicoffset.TopicOffsetService;
import uk.gov.gchq.palisade.service.topicoffset.model.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.model.TopicOffsetResponse;

import java.nio.charset.Charset;

/**
 * Simple implementation of the Topic Offset Service. This service will check to see if there is a header with the
 * start marker in the collection of headers. If this condition is met, the response will be to return true.
 */
public class SimpleTopicOffsetService implements TopicOffsetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTopicOffsetService.class);

    /**
     * Checks for the presence of the start marker in the collection of headers.
     *
     * @param headers map of headers submitted for evaluation.
     * @return boolean true if there is a start marker in the header.
     */
    @Override
    public boolean isOffsetForTopic(final Headers headers) {
        Header x = headers.lastHeader(StreamMarker.HEADER);
        boolean isOffset = ((x != null) && (new String(x.value(), Charset.defaultCharset()).equals(StreamMarker.START.toString())));
        if (isOffset) {
            LOGGER.info("Found topic offset: {}", headers);
        }
        return isOffset;
    }

    /**
     * Given an offset, calls the TopicOffsetResponse builder to create a response message
     *
     * @param offset the offset of the start marker in the topic
     * @return the topic offset response message object
     */
    @Override
    public TopicOffsetResponse createTopicOffsetResponse(final Long offset) {
        return TopicOffsetResponse.Builder.create().withOffset(offset);
    }
}
