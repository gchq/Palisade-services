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

package uk.gov.gchq.palisade.contract.attributemask.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.contract.attributemask.ContractTestData;
import uk.gov.gchq.palisade.contract.attributemask.kafka.KafkaTestConfiguration;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {RedisPersistenceContractTest.class, AttributeMaskingApplication.class},
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"spring.data.redis.repositories.timeToLive.AuthorisedRequestEntity=1s", "akka.discovery.config.services.kafka.from-config=false", "spring.data.redis.repositories.key-prefix=test:"}
)
@Import({KafkaTestConfiguration.class})
@ContextConfiguration(initializers = {RedisInitializer.class})
@ActiveProfiles({"redis", "akkatest"})
class RedisPersistenceContractTest {
    static final Logger LOGGER = LoggerFactory.getLogger(RedisPersistenceContractTest.class);

    @Autowired
    private AttributeMaskingService service;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    protected void cleanCache() {
        requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushAll();
    }

    @AfterEach
    void tearDown() {
        cleanCache();
    }

    @Test
    void testContextLoads() {
        assertThat(service).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    void testAuthorisedRequestsAreStoredInRedis() {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        AttributeMaskingRequest request = ContractTestData.REQUEST_OBJ;

        // When a request is made to store the request for a given token
        service.storeAuthorisedRequest(token, request).join();

        // Then the request is persisted in redis
        final String redisKey = "test:AuthorisedRequestEntity:" + new AuthorisedRequestEntity.AuthorisedRequestEntityId(token, request.getResourceId()).getUniqueId();
        assertThat(redisTemplate.keys(redisKey)).hasSize(1);

        // Values for the entity are correct
        final Map<Object, Object> redisHash = redisTemplate.boundHashOps(redisKey).entries();
        assertThat(redisHash)
                .containsEntry("token", ContractTestData.REQUEST_TOKEN)
                .containsEntry("resourceId", ContractTestData.REQUEST_OBJ.getResource().getId());
    }

    @Test
    void testAuthorisedRequestsAreEvictedAfterTtlExpires() throws InterruptedException {
        // Given we have some request data
        String token = ContractTestData.REQUEST_TOKEN;
        AttributeMaskingRequest request = ContractTestData.REQUEST_OBJ;

        // When a request is made to store the request for a given token
        service.storeAuthorisedRequest(token, request).join();
        TimeUnit.SECONDS.sleep(2);
        // Then the offset is persisted in redis
        assertThat(redisTemplate.keys("test:AuthorisedRequestEntity:" + new AuthorisedRequestEntity.AuthorisedRequestEntityId(token, request.getResourceId()).getUniqueId())).isEmpty();
    }

}
