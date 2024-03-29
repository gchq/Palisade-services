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
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.contract.data.config.model.Employee;
import uk.gov.gchq.palisade.contract.data.kafka.KafkaTestConfiguration;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.service.authorisation.AuthorisationService;
import uk.gov.gchq.palisade.user.User;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(
        classes = DataApplication.class,
        webEnvironment = WebEnvironment.MOCK,
        properties = {"spring.cache.redis.timeToLive=1s", "akka.discovery.config.services.kafka.from-config=false", "spring.data.redis.repositories.key-prefix=test:", "server.port=0"})
@Import({KafkaTestConfiguration.class})
@ActiveProfiles({"redis", "testcontainers"})
@EnableRedisRepositories
@ContextConfiguration(initializers = RedisInitializer.class)
class RedisPersistenceContractTest {

    @Autowired
    private AuthorisationService service;

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

        User user = new User().userId("test-user");
        LeafResource resource = new FileResource().id("file:/resource/id")
                .serialisedFormat("avro")
                .type(Employee.class.getTypeName())
                .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"));
        Context context = new Context().purpose("test-purpose");
        Rules<?> rules = new Rules<>();

        AuthorisedDataRequest authorisedDataRequest = AuthorisedDataRequest.Builder.create()
                .withResource(new FileResource().id("file:/resource/id")
                        .serialisedFormat("avro")
                        .type(Employee.class.getTypeName())
                        .connectionDetail(new SimpleConnectionDetail().serviceName("data-service")))
                .withUser(new User().userId("test-user"))
                .withContext(new Context().purpose("test-purpose"))
                .withRules(new Rules<>());
        repository.save(new AuthorisedRequestEntity(
                token,
                user,
                resource,
                context,
                rules
        ));

        // When
        DataRequest dataRequest = DataRequest.Builder.create()
                .withToken(token)
                .withLeafResourceId(resource.getId());
        CompletableFuture<AuthorisedDataRequest> futureDataResponse = service.authoriseRequest(dataRequest);
        AuthorisedDataRequest authorisedDataFromResource = futureDataResponse.join();
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
