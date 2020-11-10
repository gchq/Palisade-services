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

package uk.gov.gchq.palisade.component.attributemask.service;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
import org.testcontainers.shaded.com.fasterxml.jackson.annotation.JsonProperty;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.component.attributemask.repository.ExecutorTestConfiguration;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.attributemask.AttributeMaskingApplication;
import uk.gov.gchq.palisade.service.attributemask.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.attributemask.model.AttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingRequest;
import uk.gov.gchq.palisade.service.attributemask.model.AuditableAttributeMaskingResponse;
import uk.gov.gchq.palisade.service.attributemask.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingAspect;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.service.LeafResourceMasker;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AttributeMaskingApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {ExecutorTestConfiguration.class, AttributeMaskingServiceErrorTest.Config.class})
@EntityScan(basePackageClasses = {AuthorisedRequestEntity.class})
@EnableJpaRepositories(basePackages = {"uk.gov.gchq.palisade.service.attributemask.repository"})
public class AttributeMaskingServiceErrorTest {

    public static final Function<Integer, String> REQUEST_FACTORY_JSON = i -> String.format("{\"userId\":\"test-user-id\",\"resourceId\":\"/test/resourceId%d\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"test-purpose\"}},\"user\":{\"userId\":{\"id\":\"test-user-id\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"/test/resourceId\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-data-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"/test/\"},\"serialisedFormat\":\"avro\",\"type\":\"%d\"},\"rules\":{\"message\":\"no rules set\",\"rules\":{\"test-rule\":{\"class\":\"uk.gov.gchq.palisade.contract.attributemask.ContractTestData$PassThroughRule\"}}}}", i, i);

    public final Function<Integer, JsonNode> requestFactoryNode = i -> {
        try {
            return this.mapper.readTree(REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };

    public final Function<Integer, AttributeMaskingRequest> requestFactoryObj = i -> {
        try {
            return this.mapper.treeToValue(requestFactoryNode.apply(i), AttributeMaskingRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    };

    @Autowired
    private AttributeMaskingService attributeMaskingService;

    @Autowired
    ObjectMapper mapper;

    @Test
    public void persistenceFailureTest() {
        final AttributeMaskingRequest attributeMaskingRequest = requestFactoryObj.apply(1);

        final CompletableFuture<AuditableAttributeMaskingRequest> subject = this.attributeMaskingService.storeAuthorisedRequest("test-token", attributeMaskingRequest);

        assertThat(subject.getNow(AuditableAttributeMaskingRequest.Builder.create().withAttributeMaskingRequest(null).withNoError()).getAuditErrorMessage().getError().getMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isEqualTo("Cannot persist");

        assertThat(subject.getNow(AuditableAttributeMaskingRequest.Builder.create().withAttributeMaskingRequest(attributeMaskingRequest).withNoError()).getAttributeMaskingRequest())
                .as("verify that auditable object has no payload")
                .isNull();
    }

    @Test
    public void maskingFailureTest() {
        final AttributeMaskingRequest attributeMaskingRequest = requestFactoryObj.apply(1);

        final AuditableAttributeMaskingResponse subject = this.attributeMaskingService.maskResourceAttributes(attributeMaskingRequest);

        assertThat(subject.getAuditErrorMessage().getError().getMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isEqualTo("Cannot mask");

        assertThat(subject.getAttributeMaskingResponse())
                .as("verify that auditable object has no payload")
                .isNull();
    }

    @Test
    public void jsonFormatFailureTest() throws JsonProcessingException {
        final AttributeMaskingRequest attributeMaskingRequest = requestFactoryObj.apply(1);

        JsonNode stub;
        try {
           stub = this.mapper.readTree("{ \"value\": \"content\" }");
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }

        final AttributeMaskingRequest broken = AttributeMaskingRequest.Builder.create()
                .withUserId(attributeMaskingRequest.getUserId())
                .withResourceId(attributeMaskingRequest.getResourceId())
                .withContextNode(stub)
                .withUser(attributeMaskingRequest.getUser())
                .withResource(attributeMaskingRequest.getResource())
                .withRules(attributeMaskingRequest.getRules());

        final CompletableFuture<AuditableAttributeMaskingRequest> subject = this.attributeMaskingService.storeAuthorisedRequest("broken-token", broken);

        assertThat(subject.getNow(AuditableAttributeMaskingRequest.Builder.create().withAttributeMaskingRequest(null).withNoError()).getAuditErrorMessage().getError().getMessage())
                .as("verify that exception is propagated into an auditable object and returned")
                .isEqualTo("Missing type id when trying to resolve subtype of [simple type, class uk.gov.gchq.palisade.Context]: missing type id property 'class'\n at [Source: UNKNOWN; line: -1, column: -1]");
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

    public static class ExceptionalPersistenceLayer implements PersistenceLayer {

        @Override
        public CompletableFuture<AttributeMaskingRequest> putAsync(final String token, final User user, final LeafResource resource, final Context context, final Rules<?> rules) {
            throw new RuntimeException("Cannot persist");
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    public static class Stub {

        public String getValue() {
            return value;
        }

        @JsonProperty("value")
        private String value;

        @JsonGetter("class")
        @Generated
        public String getClassName() {
            return getClass().getName();
        }

    }

}
