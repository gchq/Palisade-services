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

package uk.gov.gchq.palisade.contract.data.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.contract.data.rest.config.DataTestConfiguration;
import uk.gov.gchq.palisade.contract.data.rest.mock.AuditServiceMock;
import uk.gov.gchq.palisade.contract.data.rest.mock.PalisadeServiceMock;
import uk.gov.gchq.palisade.contract.data.rest.model.Employee;
import uk.gov.gchq.palisade.contract.data.rest.web.DataClientWrapper;
import uk.gov.gchq.palisade.data.serialise.AvroSerialiser;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.request.ReadRequest;
import uk.gov.gchq.palisade.service.data.service.DataService;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@EnableFeignClients
@Import(DataTestConfiguration.class)
@SpringBootTest(classes = DataApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public class DataServiceRestContractTest {
    private static final ObjectMapper MAPPER = JSONSerialiser.createDefaultMapper();

    private static final String CURRENT_PATH = Paths.get("src/contract-tests/resources/data/employee_file0.avro").toUri().toString();
    private static final FileResource RESOURCE = ((FileResource) ResourceBuilder.create(CURRENT_PATH))
            .type(Employee.class.getTypeName())
            .serialisedFormat("avro");

    @Autowired
    private Map<String, DataService> serviceMap;
    @Autowired
    private DataClientWrapper client;

    private final WireMockRule auditMock = AuditServiceMock.getRule();
    private final WireMockRule palisadeMock = PalisadeServiceMock.getRule();

    private AvroSerialiser<Employee> avroSerialiser;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        AuditServiceMock.stubRule(auditMock, MAPPER);
        auditMock.start();

        PalisadeServiceMock.stubRule(palisadeMock, MAPPER, RESOURCE);
        palisadeMock.start();

        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        avroSerialiser = new AvroSerialiser<>(Employee.class);
    }

    @AfterEach
    public void tearDown() {
        auditMock.stop();
        auditMock.resetAll();

        palisadeMock.stop();
        palisadeMock.resetAll();
    }

    @Test
    public void contextLoads() {
        assertNotNull(serviceMap);
        assertNotEquals(serviceMap, Collections.emptyMap());
    }

    @Test
    public void isUp() {
        Response health = client.getHealth();
        assertThat(health.status(), equalTo(200));
    }

    @Test
    public void readChunkedTest() {
        // Given - ReadRequest created
        ReadRequest readRequest = new ReadRequest().token("token").resource(RESOURCE);
        readRequest.setOriginalRequestId(new RequestId().id("original"));

        // Given - AvroSerialiser added to Data-service
        client.addSerialiser(DataFlavour.of(Employee.class.getTypeName(), "avro"), avroSerialiser);

        // When
        Set<Employee> readResult = client.readChunked(readRequest).collect(Collectors.toSet());

        // Then
        for (Employee result : readResult) {
            assertThat(result, equalTo(new Employee()));
        }
    }
}
