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
package uk.gov.gchq.palisade.component.policy.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import uk.gov.gchq.palisade.component.policy.PolicyTestUtil;
import uk.gov.gchq.palisade.component.policy.config.PolicyTestConfiguration;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.policy.PolicyApplication;
import uk.gov.gchq.palisade.service.policy.service.PolicyService;
import uk.gov.gchq.palisade.service.policy.web.PolicyRestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PolicyTestConfiguration.class)
@ContextConfiguration(classes = PolicyApplication.class)
@WebMvcTest(PolicyRestController.class)
class PolicyRestControllerTest {

    static final String CAN_ACCESS_REQUEST_URL = "/canAccess";
    static final String GET_POLICY_SYNC_URL = "/getPolicySync";
    static final String SET_RESOURCE_POLICY_ASYNC_URL = "/setResourcePolicyAsync";
    static final String SET_TYPE_POLICY_ASYNC_URL = "/setTypePolicyAsync";

    @MockBean
    @Qualifier("controller")
    private PolicyService policyService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        Mockito.when(policyService.canAccess(PolicyTestUtil.mockUser(), PolicyTestUtil.mockContext(), PolicyTestUtil.mockResource())).thenReturn(Optional.of(PolicyTestUtil.mockResource()));
    }

    /**
     * Tests the PolicyController for the service endpoint "/canAccess"
     * It tests that the service endpoint for the following:
     * 1) request URL is "/canAccess"
     * 2) request is a doPost process
     * 3) request data is in JSON format for a CanAccessRequest object
     * 4) response data is Json format
     * 5) response includes the text canAccessResources
     * 6) response status is 200 OK
     *
     * @throws Exception if the test fails
     */
//    @Test
//    void testShouldReturnCanAccess() throws Exception {
//        //GIVEN
//        CanAccessRequest canAccessRequest = (new CanAccessRequest())
//                .context(PolicyTestUtil.mockContext())
//                .user(PolicyTestUtil.mockUser())
//                .resources(PolicyTestUtil.mockResources());
//        canAccessRequest.originalRequestId(PolicyTestUtil.mockOriginalRequestId());
//
//        //WHEN
//        MvcResult result = this.mockMvc.perform(post(CAN_ACCESS_REQUEST_URL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding(StandardCharsets.UTF_8.name())
//                .content(mapper.writeValueAsString(canAccessRequest)))
//                .andExpect(status().isOk())
//                .andDo(print())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
//                .andExpect(content().string(containsString("canAccessResources")))
//                .andReturn();
//        CanAccessResponse response = mapper.readValue(result.getResponse().getContentAsString(), CanAccessResponse.class);
//
//        //THEN
//        Mockito.verify(policyService, times(1)).canAccess(PolicyTestUtil.mockUser(), PolicyTestUtil.mockContext(), PolicyTestUtil.mockResource());
//        assertThat(response.getCanAccessResources()).contains(PolicyTestUtil.mockResource());
//    }

    /**
     * Tests the PolicyController for the service endpoint "/getPolicySync"
     * It tests that the service endpoint for the following:
     * 1) request  URL is "/getPolicySync"
     * 2) request is a doPost process
     * 3) request data is in JSON format for a GetPolicyRequest object
     * 4) response data is Json format
     * 5) status is 200 OK
     *
     * @throws Exception if the test fails
     */
//    @Test
//    void testShouldReturnPolicySync() throws Exception {
//
//        //GIVEN
//        GetPolicyRequest getPolicyRequest = (new GetPolicyRequest())
//                .context(PolicyTestUtil.mockContext())
//                .user(PolicyTestUtil.mockUser())
//                .resources(PolicyTestUtil.mockResources());
//        getPolicyRequest.originalRequestId((PolicyTestUtil.mockOriginalRequestId()));
//        String jsonGetPolicyRequestMessage = mapper.writeValueAsString(getPolicyRequest);
//
//        //WHEN
//        MvcResult result = this.mockMvc.perform(post(GET_POLICY_SYNC_URL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding(StandardCharsets.UTF_8.name())
//                .content(jsonGetPolicyRequestMessage))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
//                .andReturn();
//
//        //THEN
//        Mockito.verify(policyService, times(1)).getPolicy(PolicyTestUtil.mockResource());
//        Map<LeafResource, Rules> response = mapper.readValue(result.getResponse().getContentAsString(), Map.class);
//        assertThat(response).isNotNull();
//    }

    /**
     * Tests the PolicyController for the service endpoint "/setResourcePolicyAsync"
     * It tests that the service endpoint for the following:
     * 1) request  URL is "/setResourcePolicyAsync"
     * 2) request is a doPut process
     * 3) request data is in JSON format for a SetResourcePolicyRequest object
     * 4) response status is 200 OK
     *
     * @throws Exception if the test fails
     */
//    @Test
//    void testShouldSetResourcePolicyAsync() throws Exception {
//        //GIVEN
//        SetResourcePolicyRequest getSetResourcePolicyRequest = (new SetResourcePolicyRequest())
//                .policy(PolicyTestUtil.mockPolicy())
//                .resource(PolicyTestUtil.mockResource());
//        String jsonSetResourcePolicyRequestMessage = mapper.writeValueAsString(getSetResourcePolicyRequest);
//
//        //WHEN
//        this.mockMvc.perform(put(SET_RESOURCE_POLICY_ASYNC_URL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding(StandardCharsets.UTF_8.name())
//                .content(jsonSetResourcePolicyRequestMessage))
//                .andDo(print())
//                .andExpect(status().isOk());
//    }

    /**
     * Tests the PolicyController for the service endpoint "/setTypePolicyAsync"
     * It tests that the service endpoint for the following:
     * 1) request  URL is "/setTypePolicyAsync"
     * 2) request is a doPut process
     * 3) request data is in JSON format for a SetTypePolicyRequest object
     * 4) response status is 200 OK
     *
     * @throws Exception if the test fails
     */
//    @Test
//    void testShouldSetTypePolicyAsync() throws Exception {
//        //GIVEN
//        SetTypePolicyRequest setTypePolicyRequest = (new SetTypePolicyRequest())
//                .policy(PolicyTestUtil.mockPolicy())
//                .type("Test type");
//        String jsonSetTypePolicyAsyncMessage = mapper.writeValueAsString(setTypePolicyRequest);
//
//        //WHEN
//        this.mockMvc.perform(put(SET_TYPE_POLICY_ASYNC_URL)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding(StandardCharsets.UTF_8.name())
//                .content(jsonSetTypePolicyAsyncMessage))
//                .andDo(print())
//                .andExpect(status().isOk());
//    }
}
