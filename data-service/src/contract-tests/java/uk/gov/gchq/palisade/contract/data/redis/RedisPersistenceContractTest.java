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

package uk.gov.gchq.palisade.contract.data.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;

import uk.gov.gchq.palisade.contract.data.config.model.Employee;
import uk.gov.gchq.palisade.contract.data.redis.RedisPersistenceContractTest.Initializer;
import uk.gov.gchq.palisade.reader.common.Context;
import uk.gov.gchq.palisade.reader.common.SimpleConnectionDetail;
import uk.gov.gchq.palisade.reader.common.User;
import uk.gov.gchq.palisade.reader.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.reader.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.reader.common.rule.Rules;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.service.DataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(classes = DataApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"redis"})
@EnableRedisRepositories
@ContextConfiguration(initializers = Initializer.class)
class RedisPersistenceContractTest {
    private static final int REDIS_PORT = 6379;

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static final GenericContainer<?> REDIS = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);

        @Override
        public void initialize(@NonNull final ConfigurableApplicationContext context) {
            // Start container
            REDIS.start();

            // Override Redis configuration
            String redisContainerIP = "spring.redis.host=" + REDIS.getContainerIpAddress();
            // Configure the testcontainer random port
            String redisContainerPort = "spring.redis.port=" + REDIS.getMappedPort(REDIS_PORT);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }

    @Autowired
    private DataService service;

    @Autowired
    private AuthorisedRequestsRepository repository;

    @Test
    void testContextLoads() {
        assertThat(service).isNotNull();
    }

    @Test
    void testAuthorisedRequestsAreRetrievedFromRedis() {
        // Given
        String token = "token";

        var readerRequest = new DataReaderRequest()
                .user(new User().userId("test-user"))
                .resource(new FileResource().id("/resource/id")
                        .serialisedFormat("avro")
                        .type(Employee.class.getTypeName())
                        .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"))
                        .parent(new SystemResource().id("/")))
                .context(new Context().purpose("test-purpose"))
                .rules(new Rules<>());

        var authorisedDataRequest = AuthorisedDataRequest.Builder.create().withResource(new FileResource().id("/resource/id")
                .serialisedFormat("avro")
                .type(Employee.class.getTypeName())
                .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"))
                .parent(new SystemResource().id("/")))
                .withUser(new User().userId("test-user"))
                .withContext(new Context().purpose("test-purpose"))
                .withRules(new Rules<>());
        repository.save(new AuthorisedRequestEntity(
                token,
                readerRequest.getUser(),
                readerRequest.getResource(),
                readerRequest.getContext(),
                readerRequest.getRules()
        ));

        // When
        var dataRequest = DataRequest.Builder.create()
                .withToken(token)
                .withLeafResourceId(readerRequest.getResource().getId());
        var futureDataResponse = service.authoriseRequest(dataRequest);
        var authorisedDataFromResource = futureDataResponse.join();
        // Then
        assertAll("ObjectComparison",
                () -> assertThat(authorisedDataFromResource)
                        .as("Comparison using the DataResponse's equals method")
                        .isEqualTo(authorisedDataRequest),

                () -> assertThat(authorisedDataFromResource)
                        .as("Comparison of content using all of the DataResponse's components recursively")
                        .usingRecursiveComparison()
                        .isEqualTo(authorisedDataRequest)
        );
    }
}
