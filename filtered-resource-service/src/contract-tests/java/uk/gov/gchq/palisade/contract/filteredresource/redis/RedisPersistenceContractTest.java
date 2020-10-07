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
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.service.OffsetEventService;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = FilteredResourceApplication.class,
        webEnvironment = WebEnvironment.NONE,
        properties = "spring.data.redis.repositories.timeToLive.TokenOffsetEntity=1s"
)
@Import(RedisTestConfiguration.class)
@ActiveProfiles("redis")
class RedisPersistenceContractTest {

    @Autowired
    private OffsetEventService service;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testContextLoads() {
        assertThat(service).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    void testTopicOffsetsAreStoredInRedis() {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        TopicOffsetMessage request = ContractTestData.TOPIC_OFFSET_MESSAGE;

        // When a request is made to store the topic offset for a given token
        service.storeTokenOffset(token, request.queuePointer).join();

        // Then the offset is persisted in redis
        assertThat(redisTemplate.keys("TokenOffsetEntity:" + token)).hasSize(1);
    }

    @Test
    void testTopicOffsetsAreEvictedAfterTtlExpires() throws InterruptedException {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        TopicOffsetMessage request = ContractTestData.TOPIC_OFFSET_MESSAGE;

        // When a request is made to store the topic offset for a given token
        service.storeTokenOffset(token, request.queuePointer).join();
        TimeUnit.SECONDS.sleep(1);

        // Then the offset is persisted in redis
        assertThat(redisTemplate.keys("TokenOffsetEntity:" + token)).isEmpty();
    }

}
