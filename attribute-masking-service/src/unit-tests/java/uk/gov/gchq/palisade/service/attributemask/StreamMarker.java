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

package uk.gov.gchq.palisade.service.attributemask;

/**
 * Marks the start or end of a stream of messages.
 * Will be present in headers to indicate the message is empty and marks the start/end of the stream.
 * Will not be present for all other (content-ful) messages.
 */
public enum StreamMarker {
    START,
    END;

    public static final String HEADER = "x-stream-marker";
}
