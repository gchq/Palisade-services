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
package uk.gov.gchq.palisade.service.topicoffset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Topic Offset Service is a performance optimisation for the stream message process.  The service will look for the
 * indication that this message is the first of a set of response messages for a specific request. It will
 * be watching for a Kafka header with the message {Stream-Marker=Start, RequestId=xxxx-xxxx-xxxx}. It will take this
 * information along with the commit offset of this stream and this will be written to the downstream queue.  This can
 * then be used to optimise the start up client connections by the results-service.
 */
@SpringBootApplication
@ComponentScan
public class TopicOffsetApplication {

    /**
     * Starts the Topic Offset Service
     * @param args required input for the main method
     */
    public static void main(final String[] args) {
        SpringApplication.run(TopicOffsetApplication.class, args);
    }
}
