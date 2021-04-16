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

package uk.gov.gchq.palisade.component.attributemask.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import uk.gov.gchq.palisade.component.attributemask.repository.ExecutorTestConfiguration;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.common.Context;
import uk.gov.gchq.palisade.service.attributemask.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.attributemask.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.attributemask.common.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.common.user.User;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingAspect;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.LeafResourceMasker;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify the handling of exceptions, and the population of audit objects during stream processing
 */
@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {ExecutorTestConfiguration.class, AttributeMaskingServiceErrorTest.Config.class})
@EntityScan(basePackageClasses = {AuthorisedRequestEntity.class})
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.attributemask.repository"})
class AttributeMaskingServiceErrorTest {

    private static final Function<Integer, String> REQUEST_FACTORY_JSON = i -> String.format("{\"userId\":\"test-user-id\",\"resourceId\":\"/test/resourceId%d\",\"context\":{\"@type\":\"Context\",\"contents\":{\"purpose\":\"test-purpose\"}},\"user\":{\"userId\":{\"id\":\"test-user-id\"},\"roles\":[],\"auths\":[],\"@type\":\"User\"},\"resource\":{\"@type\":\"FileResource\",\"id\":\"/test/resourceId\",\"attributes\":{},\"connectionDetail\":{\"@type\":\"SimpleConnectionDetail\",\"serviceName\":\"test-data-service\"},\"parent\":{\"@type\":\"SystemResource\",\"id\":\"/test/\"},\"serialisedFormat\":\"avro\",\"type\":\"%d\"},\"rules\":{\"message\":\"no rules set\",\"rules\":{\"test-rule\":{\"@type\":\"ContractTestData$PassThroughRule\"}}}}", i, i);

    private final Function<Integer, JsonNode> requestFactoryNode = i -> {
        try {
            return this.mapper.readTree(REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };

    private final Function<Integer, AttributeMaskingRequest> requestFactoryObj = i -> {
        try {
            return this.mapper.treeToValue(requestFactoryNode.apply(i), AttributeMaskingRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    };

    @Autowired
    private AttributeMaskingService attributeMaskingService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void testPersistenceFailure() {
        // Given a masking request
        final var attributeMaskingRequest = requestFactoryObj.apply(1);
        // When persisting
        final var request = this.attributeMaskingService.storeAuthorisedRequest("test-token", attributeMaskingRequest).join();

        // Then the service suppresses exception and populates Audit object
        assertThat(request)
                .as("Check that the request has an error and that the error message has been populated successfully")
                .extracting(AuditableAttributeMaskingRequest::getAuditErrorMessage)
                .extracting(AuditErrorMessage::getError)
                .isInstanceOf(Throwable.class)
                .extracting(Throwable::getMessage)
                .isEqualTo("Cannot persist");

        assertThat(request)
                .as("Check that the request has a null AttributeMaskingRequest")
                .extracting(AuditableAttributeMaskingRequest::getAttributeMaskingRequest)
                .isNull();
    }

    @Test
    void testMaskingFailure() {
        // Given a masking request
        final var attributeMaskingRequest = requestFactoryObj.apply(1);
        // When masking
        final var response = this.attributeMaskingService.maskResourceAttributes(attributeMaskingRequest);
        // Then the service suppresses exception and populates Audit object
        assertThat(response)
                .as("verify that exception is propagated into an auditable object and returned")
                .extracting(AuditableAttributeMaskingResponse::getAuditErrorMessage)
                .isNotNull()
                .extracting(AuditErrorMessage::getError)
                .extracting(Throwable::getMessage)
                .isEqualTo("Cannot mask");

        assertThat(response)
                .as("verify that auditable object has no payload")
                .extracting(AuditableAttributeMaskingResponse::getAttributeMaskingResponse)
                .isNull();
    }

    @Test
    void testJsonFormatFailure() throws JsonProcessingException {
        // Given a masking request
        var attributeMaskingRequest = requestFactoryObj.apply(1);
        // With invalid JSON in the context
        JsonNode stub;
        try {
            stub = this.mapper.readTree("{ \"value\": \"content\" }");
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }

        var broken = AttributeMaskingRequest.Builder.create()
                .withUserId(attributeMaskingRequest.getUserId())
                .withResourceId(attributeMaskingRequest.getResourceId())
                .withContextNode(stub)
                .withUser(attributeMaskingRequest.getUser())
                .withResource(attributeMaskingRequest.getResource())
                .withRules(attributeMaskingRequest.getRules());
        // When persisting
        var request = this.attributeMaskingService.storeAuthorisedRequest("broken-token", broken).join();
        // Then the service suppresses exception and populates Audit object
        assertThat(request)
                .as("verify that exception is propagated into an auditable object and returned")
                .extracting(AuditableAttributeMaskingRequest::getAuditErrorMessage)
                .extracting(AuditErrorMessage::getError)
                .extracting(Throwable::getMessage)
                .isEqualTo("Cannot persist");
    }

    @Configuration
    public static class Config {

        @Primary
        @Bean
        ExceptionalPersistenceLayer persistenceLayer() {
            return new ExceptionalPersistenceLayer();
        }

        @Primary
        @Bean
        LeafResourceMasker simpleLeafResourceMasker() {
            // Delete all additional attributes (if a FileResource)
            return (LeafResource x) -> {
                if (x instanceof FileResource) {
                    throw new RuntimeException("Cannot mask");
                } else {
                    return x;
                }
            };
        }

        @Primary
        @Bean
        AttributeMaskingService simpleAttributeMaskingService(final ExceptionalPersistenceLayer persistenceLayer, final LeafResourceMasker resourceMasker) {
            return new AttributeMaskingService(persistenceLayer, resourceMasker);
        }

        @Primary
        @Bean
        AttributeMaskingAspect attributeMaskingAspect() {
            return new AttributeMaskingAspect();
        }

    }

    private static class ExceptionalPersistenceLayer implements PersistenceLayer {

        @Override
        public CompletableFuture<AttributeMaskingRequest> putAsync(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
            throw new RuntimeException("Cannot persist");
        }
    }

}
