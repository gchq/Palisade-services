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

package uk.gov.gchq.palisade.contract.filteredresource.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.contract.filteredresource.ContractTestData;
import uk.gov.gchq.palisade.contract.filteredresource.redis.config.RedisTestConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.FilteredResourceApplication;
import uk.gov.gchq.palisade.service.filteredresource.domain.TokenOffsetEntity;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.web.FilteredResourceController;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FilteredResourceApplication.class, webEnvironment = WebEnvironment.NONE)
@Import(RedisTestConfiguration.class)
@ActiveProfiles("redis")
class RedisPersistenceContractTest {

    @Autowired
    private FilteredResourceController controller;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testContextLoads() {
        assertThat(controller).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    void testTopicOffsetsAreStoredInRedis() throws JsonProcessingException {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        TopicOffsetMessage request = ContractTestData.TOPIC_OFFSET_MESSAGE;

        // When a request is made to store the topic offset for a given token
        controller.storeTopicOffset(token, request);

        // Then the offset is persisted in redis
        assertThat(redisTemplate.keys(TokenOffsetEntity.class.getSimpleName())).hasSize(1);
    }

}
