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

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Simple implementation of the Topic Offset Service. This service will check to see if there is a header with the
 * start marker in the collection of headers. If this condition is met, the response will be to return true.
 */
public class SimpleTopicOffsetService implements TopicOffsetService {

    //private final Predicate<Map<String, String>> predicate = headers -> headers.get(StreamMarker.HEADER)).equals(StreamMarker.START.toString());

    /**
     * Checks for the presence of the start marker in the collection of headers.
     *
     * @param headers map of headers submitted for evaluation.
     * @return boolean true if there is a start marker in the header.
     */
    @Override
    public boolean isOffsetForTopic(final Map<String, String> headers) {
        boolean found = false;
        if(headers.containsKey(StreamMarker.HEADER) && (headers.get(StreamMarker.HEADER).equals(StreamMarker.START.toString()))){
            found =true;
        }
      return found;
    }
}
