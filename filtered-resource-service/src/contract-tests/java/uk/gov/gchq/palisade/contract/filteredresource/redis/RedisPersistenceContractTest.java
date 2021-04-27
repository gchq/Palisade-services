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

package uk.gov.gchq.palisade.contract.filteredresource.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.AutoConfigureDataRedis;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.contract.filteredresource.ContractTestData;
import uk.gov.gchq.palisade.service.filteredresource.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.config.AsyncConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.config.RedisConfiguration;
import uk.gov.gchq.palisade.service.filteredresource.model.TopicOffsetMessage;
import uk.gov.gchq.palisade.service.filteredresource.repository.offset.JpaTokenOffsetPersistenceLayer;
import uk.gov.gchq.palisade.service.filteredresource.service.OffsetEventService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.data.redis.repositories.timeToLive.TokenOffsetEntity=1s", "spring.data.redis.repositories.key-prefix=test:"})
@ContextConfiguration(
        classes = {ApplicationConfiguration.class, AsyncConfiguration.class, RedisConfiguration.class, JpaTokenOffsetPersistenceLayer.class},
        initializers = RedisInitializer.class
)
@EnableAutoConfiguration
@AutoConfigureDataRedis
@ActiveProfiles("redis")
class RedisPersistenceContractTest {

    protected void cleanCache() {
        requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        cleanCache();
    }

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
        service.storeTokenOffset(token, request.commitOffset).join();

        // Then the offset is persisted in redis
        final String redisKey = "test:TokenOffsetEntity:" + token;
        assertThat(redisTemplate.keys(redisKey)).hasSize(1);

        // Values for the entity are correct
        final Map<Object, Object> redisHash = redisTemplate.boundHashOps(redisKey).entries();
        assertThat(redisHash)
                .containsEntry("token", ContractTestData.REQUEST_TOKEN)
                .containsEntry("offset", ContractTestData.TOPIC_OFFSET_MESSAGE.commitOffset.toString());
    }

    @Test
    void testTopicOffsetsAreEvictedAfterTtlExpires() throws InterruptedException {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        TopicOffsetMessage request = ContractTestData.TOPIC_OFFSET_MESSAGE;

        // When a request is made to store the topic offset for a given token
        service.storeTokenOffset(token, request.commitOffset).join();
        TimeUnit.SECONDS.sleep(1);

        // Then the offset is persisted in redis
        assertThat(redisTemplate.keys("test:TokenOffsetEntity:" + token)).isEmpty();
    }

}
