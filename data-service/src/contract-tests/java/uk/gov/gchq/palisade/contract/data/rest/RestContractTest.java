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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.gchq.palisade.contract.data.config.DataTestConfiguration;
import uk.gov.gchq.palisade.contract.data.config.web.DataClient;
import uk.gov.gchq.palisade.contract.data.config.web.DataClientWrapper;
import uk.gov.gchq.palisade.contract.data.model.Employee;
import uk.gov.gchq.palisade.data.serialise.AvroSerialiser;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.service.DataService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@EnableFeignClients(basePackageClasses = {DataClient.class})
@Import(DataTestConfiguration.class)
@SpringBootTest(classes = DataApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("web")
class RestContractTest {
    private static final ObjectMapper MAPPER = JSONSerialiser.createDefaultMapper();

    private static final String CURRENT_PATH = Paths.get("src/contract-tests/resources/data/employee_file0.avro").toUri().toString();

    @Autowired
    private Map<String, DataService> serviceMap;
    @Autowired
    private DataClientWrapper client;

    private AvroSerialiser<Employee> avroSerialiser;

    @BeforeEach
    void setUp() {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        avroSerialiser = new AvroSerialiser<>(Employee.class);
    }

    @Test
    void testContextLoads() {
        assertThat(serviceMap)
                .isNotNull()
                .isNotEmpty();
        assertThat(client).isNotNull();
    }

    @Test
    void testIsUp() {
        Response health = client.getHealth();
        assertThat(health.status()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void testReadChunked() throws IOException {
        // Given - DataRequest created
        DataRequest readRequest = DataRequest.Builder.create()
                .withToken("token")
                .withLeafResourceId(CURRENT_PATH);

        // Given - AvroSerialiser added to Data-service
        client.addSerialiser(DataFlavour.of(Employee.class.getTypeName(), "avro"), avroSerialiser);

        // Given - the file contains the expected data
        avroSerialiser.serialise(Stream.of(new Employee()), new FileOutputStream("src/contract-tests/resources/data/employee_file0.avro"));

        // When
        Set<Employee> readResult = client.readChunked(readRequest).collect(Collectors.toSet());

        // Then
        for (Employee result : readResult) {
            assertThat(result).isEqualTo(new Employee());
        }
    }
}
