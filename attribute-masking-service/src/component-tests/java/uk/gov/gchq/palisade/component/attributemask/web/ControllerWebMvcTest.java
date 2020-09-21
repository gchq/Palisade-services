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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import uk.gov.gchq.palisade.service.attributemask.ApplicationTestData;
import uk.gov.gchq.palisade.service.attributemask.message.Token;
import uk.gov.gchq.palisade.service.attributemask.service.AttributeMaskingService;
import uk.gov.gchq.palisade.service.attributemask.web.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(controllers = {RestController.class})
@ContextConfiguration(classes = {ControllerWebMvcTest.class, RestController.class})
class ControllerWebMvcTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @MockBean
    private AttributeMaskingService mockService;

    @Autowired
    private RestController controller;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testContextLoads() {
        assertThat(controller).isNotNull();
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void testControllerPersistsAndMasks() throws Exception {
        // Given some application test data
        Mockito.when(mockService.maskResourceAttributes(any())).thenReturn(ApplicationTestData.LEAF_RESOURCE);

        // When a request comes in to the controller
        mockMvc.perform(MockMvcRequestBuilders.post("/streamApi/maskAttributes")
                .header(Token.HEADER, ApplicationTestData.REQUEST_TOKEN)
                .content(MAPPER.writeValueAsBytes(ApplicationTestData.REQUEST))
                .contentType("application/json"))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content().bytes(MAPPER.writeValueAsBytes(ApplicationTestData.RESPONSE)));
    }

}
