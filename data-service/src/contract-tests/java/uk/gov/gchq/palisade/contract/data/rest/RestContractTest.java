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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.contract.data.config.model.Employee;
import uk.gov.gchq.palisade.data.serialise.AvroSerialiser;
import uk.gov.gchq.palisade.reader.common.DataFlavour;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.data.DataApplication;
import uk.gov.gchq.palisade.service.data.domain.AuthorisedRequestEntity;
import uk.gov.gchq.palisade.service.data.model.DataRequest;
import uk.gov.gchq.palisade.service.data.repository.AuthorisedRequestsRepository;
import uk.gov.gchq.palisade.service.data.service.DataService;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DataApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureDataJpa
@ActiveProfiles({"static"})
class RestContractTest {
    private static final String CURRENT_PATH = Paths.get("src/contract-tests/resources/data/employee_file0.avro").toUri().toString();
    private static final AvroSerialiser<Employee> AVRO_SERIALISER = new AvroSerialiser<>(Employee.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private DataService dataService;
    @Autowired
    private AuthorisedRequestsRepository requestsRepository;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Given - AvroSerialiser added to data-service
        dataService.addSerialiser(DataFlavour.of(Employee.class.getTypeName(), "avro"), AVRO_SERIALISER);

        // Given - Request for "token" and CURRENT_PATH is authorised
        requestsRepository.save(new AuthorisedRequestEntity(
                "token",
                new User().userId("test-user"),
                new FileResource().id(CURRENT_PATH)
                        .serialisedFormat("avro")
                .type(Employee.class.getTypeName())
                .connectionDetail(new SimpleConnectionDetail().serviceName("data-service"))
                .parent(new SystemResource().id("/")),
                new Context(),
                new Rules<>()
        ));
    }

    @Test
    void testContextLoads() {
        assertThat(dataService).isNotNull();
        assertThat(restTemplate).isNotNull();
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void testIsUp() {
        ResponseEntity<JsonNode> health = restTemplate.getForEntity("/actuator/health", JsonNode.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testReadChunked() throws Exception {
        // Given - the file contains the expected data
        // AVRO_SERIALISER.serialise(Stream.of(new Employee()), new FileOutputStream(CURRENT_PATH));

        // When
        DataRequest readRequest = DataRequest.Builder.create()
                .withToken("token")
                .withLeafResourceId(CURRENT_PATH);
        byte[] bytes = mockMvc.perform(
                post("/read/chunked")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsBytes(readRequest)))
                .andExpect(request().asyncStarted())
                .andDo(MvcResult::getAsyncResult)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        List<Employee> data = AVRO_SERIALISER.deserialise(new ByteArrayInputStream(bytes))
                .collect(Collectors.toList());

        assertThat(data)
                .hasSize(1)
                .first().isEqualTo(new Employee());
    }
}
