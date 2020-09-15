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
package uk.gov.gchq.palisade.service.topicoffset.service;

import uk.gov.gchq.palisade.service.topicoffset.message.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.message.TopicOffsetResponse;

/**
 * Service contract for the Topic Offset Service.
 */
public interface TopicOffsetService {

    /**
     * Creates a Topic Offset Response - a start message with an offset to indicate the location in a message queue.
     * If the {@link StreamMarker} is found to be a start message, the response will be a {@link TopicOffsetResponse}
     * with th offset information.  For messages that have an end marker or are messages with resource data,
     * it will not create a response.
     * @param streamMarker {@link StreamMarker}  indicator of the type of message marker.
     * @return  message indicating the offset in the message queue.
     */
    TopicOffsetResponse createTopicOffsetResponse(StreamMarker streamMarker);
}

