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

package uk.gov.gchq.palisade.contract.audit.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;

import uk.gov.gchq.palisade.service.audit.AuditApplication;
import uk.gov.gchq.palisade.service.audit.config.AuditServiceConfigProperties;
import uk.gov.gchq.palisade.service.audit.service.AuditService;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.BAD_SUCCESS_REQUEST_OBJ;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.ERROR_REQUEST_OBJ;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.GOOD_SUCCESS_REQUEST_OBJ;
import static uk.gov.gchq.palisade.contract.audit.ContractTestData.REQUEST_TOKEN;
import static uk.gov.gchq.palisade.service.audit.common.Token.HEADER;

/**
 * An external requirement of the service is to connect to a pair of upstream kafka topics.
 * <ol>
 *     <li>The "error" topic can be written to by any service that encounters an error when processing a request</li>
 *     <li>The "success" topic should only be written to by the filtered-resource-service or the data-service</li>
 * </ol>
 * This service does not write to a downstream topic
 */
@SpringBootTest(
        classes = AuditApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"akka.discovery.config.services.kafka.from-config=false"}
)
@Import(KafkaInitializer.Config.class)
@ContextConfiguration(initializers = {KafkaInitializer.class})
@ActiveProfiles({"akka-test"})
class KafkaContractRestTest {

    static final Logger LOGGER = LoggerFactory.getLogger(KafkaContractRestTest.class);

    @Autowired
    AuditServiceConfigProperties auditServiceConfigProperties;

    @Autowired
    TestRestTemplate restTemplate;

    @SpyBean
    private AuditService auditService;

    @AfterEach
    void tearDown() {
        Arrays.stream(new File(auditServiceConfigProperties.getErrorDirectory()).listFiles())
            .filter(file -> (file.getName().startsWith("Success") || file.getName().startsWith("Error")))
            .peek(file -> LOGGER.info("Deleting file {}", file.getName()))
            .forEach(File::deleteOnExit);
    }

    @Test
    @DirtiesContext
    void testRestEndpointForErrorMessage() throws InterruptedException {

        // When - we POST to the rest endpoint
        var responseEntity = post("/api/error", ERROR_REQUEST_OBJ);

        // Then - check the REST request was accepted
        assertThat(responseEntity.getStatusCode()).isEqualTo(ACCEPTED);

        // Then - check the audit service has invoked the audit method
        verify(auditService, timeout(3000).times(1)).audit(anyString(), any());

    }

    @Test
    @DirtiesContext
    void testRestEndpointForGoodSuccessMessage() throws InterruptedException {

        // When - we POST to the rest endpoint
        var responseEntity = post("/api/success", GOOD_SUCCESS_REQUEST_OBJ);

        // Then - check the REST request was accepted
        assertThat(responseEntity.getStatusCode()).isEqualTo(ACCEPTED);

        // Then - check the audit service has invoked the audit method
        Mockito.verify(auditService, Mockito.timeout(3000).times(1)).audit(anyString(), any());

    }

    @Test
    @DirtiesContext
    void testRestEndpointForBadSuccessMessage() throws InterruptedException {

        // When - we POST to the rest endpoint
        var responseEntity = post("/api/success", BAD_SUCCESS_REQUEST_OBJ);

        // Then - check the REST request was accepted
        assertThat(responseEntity.getStatusCode()).isEqualTo(ACCEPTED);

        // Then - check the audit service has invoked the audit method
        verify(auditService, timeout(3000).times(0)).audit(anyString(), any());

    }

    private ResponseEntity<Void> post(final String url, final Object body) throws InterruptedException {
        var headers = singletonMap(HEADER, singletonList(REQUEST_TOKEN));
        var httpEntity = new HttpEntity<>(body, new LinkedMultiValueMap<>(headers));
        var responseEntity = restTemplate.postForEntity(url, httpEntity, Void.class);
        waitForService();
        return responseEntity;
    }

    private void waitForService() throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
    }

}
