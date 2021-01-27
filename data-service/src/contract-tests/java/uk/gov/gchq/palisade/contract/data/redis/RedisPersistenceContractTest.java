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

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.contract.data.config.model.Employee;
import uk.gov.gchq.palisade.contract.data.redis.RedisPersistenceContractTest.Initializer;
import uk.gov.gchq.palisade.reader.request.DataReaderRequest;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.model.DataReaderRequestModel;
import uk.gov.gchq.palisade.service.data.model.DataRequestModel;
import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.service.DataService;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DataApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"redis"})
@EnableRedisRepositories
@ContextConfiguration(initializers = Initializer.class)
class RedisPersistenceContractTest {
    private static final int REDIS_PORT = 6379;

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(REDIS_PORT)
                .withReuse(true);

        @Override
        public void initialize(@NonNull final ConfigurableApplicationContext context) {
            // Start container
            redis.start();

            // Override Redis configuration
            String redisContainerIP = "spring.redis.host=" + redis.getContainerIpAddress();
            // Configure the testcontainer random port
            String redisContainerPort = "spring.redis.port=" + redis.getMappedPort(REDIS_PORT);
            // Override the configuration at runtime
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, redisContainerIP, redisContainerPort);
        }
    }

    @Autowired
    private DataService service;

    @Autowired
    private AuthorisedRequestsRepository repository;

    //@Test
    void testContextLoads() {
        assertThat(service).isNotNull();
    }

    //  @Test
    void testAuthorisedRequestsAreRetrievedFromRedis() {
        // Given
        String token = "token";
        DataReaderRequest readerRequest = new DataReaderRequest()
                .user(new User().userId("test-user"))
                .resource(new FileResource().id("/resource/id")
                        .serialisedFormat("avro")
                        .type(Employee.class.getTypeName())
                        .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"))
                        .parent(new SystemResource().id("/")))
                .context(new Context().purpose("test-purpose"))
                .rules(new Rules<>());
        repository.save(new AuthorisedRequestEntity(
                token,
                readerRequest.getUser(),
                readerRequest.getResource(),
                readerRequest.getContext(),
                readerRequest.getRules()
        ));

        //should   be using the PersistenceLayer not the SimpleDataService

        // When
        DataRequestModel dataRequestModel = DataRequestModel.Builder.create()
                .withToken(token)
                .withLeafResourceId(readerRequest.getResource().getId());
        CompletableFuture<DataReaderRequestModel> futureDataReaderRequestModel = service.authoriseRequest(dataRequestModel);
        DataReaderRequestModel dataReaderRequestModel = futureDataReaderRequestModel.join();
        // Then
        /*
        assertThat(isRequestAuthorised.join())
                .isPresent()
                .get()
                .isEqualTo(readerRequest);
                */

    }
}
