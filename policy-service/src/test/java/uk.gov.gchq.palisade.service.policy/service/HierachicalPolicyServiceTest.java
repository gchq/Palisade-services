/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.policy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.exception.NoConfigException;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.service.ServiceState;
import uk.gov.gchq.palisade.service.policy.repository.BackingStore;
import uk.gov.gchq.palisade.service.policy.repository.SimpleCacheService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;


@RunWith(JUnit4.class)
public class HierachicalPolicyServiceTest {

    public final ObjectMapper mapper = JSONSerialiser.createDefaultMapper();

    @Test(expected = NoConfigException.class)
    public void applyConfigFromTestException() {

        //given
        String serialisedCache = "SerialisedCache";
        CacheService cacheService = Mockito.mock(CacheService.class);
        ServiceState serviceState = Mockito.mock(ServiceState.class);
        when(serviceState.getOrDefault(any(String.class), isNull())).thenReturn(null);
        HierarchicalPolicyService policyService = new HierarchicalPolicyService(cacheService);

        //when
        policyService.applyConfigFrom(serviceState);

        //then
    }

    @Test
    public void applyConfigFromTest() {

        //given
        SimpleCacheService cacheService = new SimpleCacheService();
        BackingStore backingStore = new HeartbeatTestBackingStore();
        cacheService.backingStore(backingStore);
        ServiceState serviceState = Mockito.mock(ServiceState.class);
        String serialisedCache = new String(JSONSerialiser.serialise(cacheService));
        when(serviceState.getOrDefault(any(String.class), isNull())).thenReturn(serialisedCache);
        HierarchicalPolicyService policyService = new HierarchicalPolicyService(cacheService);

        //when
        policyService.applyConfigFrom(serviceState);

        //then
        assertThat(policyService.getCacheService().getClass(), equalTo(cacheService.getClass()));

    }


}

