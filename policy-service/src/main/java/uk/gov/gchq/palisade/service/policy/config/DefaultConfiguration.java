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

package uk.gov.gchq.palisade.service.policy.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.policy.service.NullPolicyService;
import uk.gov.gchq.palisade.service.policy.service.PolicyService;

/**
 * The default configuration beans for the Policy Service
 */
@Configuration
public class DefaultConfiguration {
    /**
     * The simplest implementation of a Policy service, allows unit tests and small services to use a lightweight Policy service
     *
     * @return a new instance of the nullPolicyService
     */
    @Bean
    @ConditionalOnProperty(prefix = "policy", name = "implementation", havingValue = "null", matchIfMissing = true)
    public PolicyService nullPolicyService() {
        return new NullPolicyService();
    }

    /**
     * A container for a number of {@link StdPolicyPrepopulationFactory} builders used for creating Policies
     * These wil be populated further using a {@code uk.gov.gchq.palisade.service.UserConfiguration} and {@code uk.gov.gchq.palisade.service.ResourceConfiguration}
     * These policies will be used for prepopulating the {@link PolicyService}
     *
     * @return a standard {@link uk.gov.gchq.palisade.service.policy.config.PolicyConfiguration} containing a list of {@link uk.gov.gchq.palisade.service.policy.config.PolicyPrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "policyProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdPolicyConfiguration policyConfiguration() {
        return new StdPolicyConfiguration();
    }

    /**
     * A factory for a map of {@link uk.gov.gchq.palisade.rule.Rules} to a resourceId, using:
     * - a {@link String} value of the resourceId
     * - a list of {@link uk.gov.gchq.palisade.rule.Rule} resource-level rules operating on a {@link uk.gov.gchq.palisade.resource.Resource}
     * - a list of {@link uk.gov.gchq.palisade.rule.Rule} record-level rules operating on the type of a {@link uk.gov.gchq.palisade.resource.LeafResource}
     *
     * @return a standard {@link uk.gov.gchq.palisade.service.policy.config.PolicyPrepopulationFactory} capable of building a Policy from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "policyProvider", havingValue = "std", matchIfMissing = true)
    public StdPolicyPrepopulationFactory policyPrepopulationFactory() {
        return new StdPolicyPrepopulationFactory();
    }
}
