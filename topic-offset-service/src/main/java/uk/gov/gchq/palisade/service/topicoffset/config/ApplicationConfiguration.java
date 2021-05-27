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
package uk.gov.gchq.palisade.service.topicoffset.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.topicoffset.service.SimpleTopicOffsetService;
import uk.gov.gchq.palisade.service.topicoffset.service.TopicOffsetService;

/**
 * Spring configuration of the Topic Offset Service. Used to define Spring Beans needed in the service.
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * Creates a new SimpleTopicOffsetService
     *
     * @return a new instance of a TopicOffsetService
     */
    @Bean
    TopicOffsetService topicOffsetService() {
        return new SimpleTopicOffsetService();
    }
}
