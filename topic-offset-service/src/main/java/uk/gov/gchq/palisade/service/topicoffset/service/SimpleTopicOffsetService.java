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
 * Simple Implementation of the Topic Offset Service contract.
 */
public class SimpleTopicOffsetService implements TopicOffsetService {

    /**
     * Creates a Topic Offset Response when there is a {@link StreamMarker} for a start of the messages related
     * to a specific data request.
     * At the moment it is hardcoded and needs to be updated.
     *
     * @param streamMarker {@link StreamMarker} indicator of the type of message marker.
     * @return class {@link TopicOffsetResponse} a message indicating the start of the set of messages for a data
     * request
     */
    @Override
    public TopicOffsetResponse createTopicOffsetResponse(final StreamMarker streamMarker) {
        TopicOffsetResponse response = null;

        //where do we get the offset?
        if ((streamMarker != null) && (streamMarker == StreamMarker.START)) {
            response = TopicOffsetResponse.Builder.create().withOffset(11111111L);
        }
        return response;
    }
}
