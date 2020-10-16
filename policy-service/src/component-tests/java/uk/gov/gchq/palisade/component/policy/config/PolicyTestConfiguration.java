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
package uk.gov.gchq.palisade.component.policy.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import uk.gov.gchq.palisade.service.PolicyConfiguration;
import uk.gov.gchq.palisade.service.ResourceConfiguration;
import uk.gov.gchq.palisade.service.UserConfiguration;
import uk.gov.gchq.palisade.service.policy.config.StdPolicyConfiguration;
import uk.gov.gchq.palisade.service.policy.config.StdResourceConfiguration;
import uk.gov.gchq.palisade.service.policy.config.StdUserConfiguration;

/**
 * Configuration class used to load class into the Application Context specifically needed for the tests.
 */

@TestConfiguration
public class PolicyTestConfiguration {

    @Bean
    public PolicyConfiguration policyConfiguration() {
        return new StdPolicyConfiguration();
    }

    @Bean
    public UserConfiguration userConfiguration() {
        return new StdUserConfiguration();
    }

    @Bean
    public ResourceConfiguration resourceConfiguration() {
        return new StdResourceConfiguration();
    }
}

