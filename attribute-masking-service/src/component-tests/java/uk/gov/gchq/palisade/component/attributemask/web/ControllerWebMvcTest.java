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

package uk.gov.gchq.palisade.component.attributemask.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.model.Token;
import uk.gov.gchq.palisade.service.attributemask.stream.ConsumerTopicConfiguration;
import uk.gov.gchq.palisade.service.attributemask.stream.config.AkkaComponentsConfig;
import uk.gov.gchq.palisade.service.attributemask.stream.config.AkkaSystemConfig;
import uk.gov.gchq.palisade.service.attributemask.web.AttributeMaskingRestController;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = {AttributeMaskingRestController.class})
@ContextConfiguration(classes = {ControllerWebMvcTest.class, AttributeMaskingRestController.class, AkkaComponentsConfig.class, AkkaSystemConfig.class, ConsumerTopicConfiguration.class})
class ControllerWebMvcTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AttributeMaskingRestController controller;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testContextLoads() {
        assertThat(controller).isNotNull();
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void testControllerPersistsAndMasks() throws Exception {
        // When a request comes in to the controller
        mockMvc.perform(MockMvcRequestBuilders.post("/streamApi/maskAttributes")
                .header(Token.HEADER, ApplicationTestData.REQUEST_TOKEN)
                .content(MAPPER.writeValueAsBytes(ApplicationTestData.REQUEST))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isAccepted());
    }

}
