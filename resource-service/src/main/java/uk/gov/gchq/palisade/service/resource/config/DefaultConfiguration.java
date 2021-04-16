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

package uk.gov.gchq.palisade.service.resource.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.resource.common.resource.ConnectionDetail;
import uk.gov.gchq.palisade.service.resource.common.resource.Resource;
import uk.gov.gchq.palisade.service.resource.common.resource.ResourceConfiguration;
import uk.gov.gchq.palisade.service.resource.common.resource.ResourcePrepopulationFactory;
import uk.gov.gchq.palisade.service.resource.common.resource.ResourceService;
import uk.gov.gchq.palisade.service.resource.common.util.ResourceBuilder;
import uk.gov.gchq.palisade.service.resource.service.SimpleResourceService;

/**
 * Overridable beans for the resource-service.
 * Additional classpath-jars may override these for implementation-specific behaviour.
 */
@Configuration
public class DefaultConfiguration {

    private final ResourceServiceConfigProperties resourceServiceConfigProperties;

    @Value("${web.client.data-service:data-service}")
    private String dataServiceName;

    /**
     * Spring dependency-injection for dependant configs
     *
     * @param resourceServiceConfigProperties service-specific config
     */
    public DefaultConfiguration(final ResourceServiceConfigProperties resourceServiceConfigProperties) {
        this.resourceServiceConfigProperties = resourceServiceConfigProperties;
    }

    /**
     * A bean for the implementation of the SimpleResourceService which is a simple implementation of
     * {@link ResourceService}
     *
     * @return a new instance of SimpleResourceService with a string value dataServiceName retrieved from the relevant profiles yaml
     */
    @Bean("simpleResourceService")
    @ConditionalOnProperty(prefix = "resource", name = "implementation", havingValue = "simple", matchIfMissing = true)
    public ResourceService simpleResourceService() {
        return new SimpleResourceService(dataServiceName, resourceServiceConfigProperties.getDefaultType());
    }

    /**
     * A container for a number of {@link StdResourcePrepopulationFactory} builders used for creating {@link Resource}s
     * These resources will be used for prepopulating the {@link ResourceService}
     *
     * @return a standard {@link ResourceConfiguration} containing a list of {@link ResourcePrepopulationFactory}s
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resourceProvider", havingValue = "std", matchIfMissing = true)
    @ConfigurationProperties(prefix = "population")
    public StdResourceConfiguration resourceConfiguration() {
        return new StdResourceConfiguration();
    }

    /**
     * A factory for {@link Resource} objects, wrapping the {@link ResourceBuilder} with a type and serialisedFormat
     * Note that this does not include resolving an appropriate {@link ConnectionDetail}, this is handled elsewhere
     *
     * @return a standard {@link ResourcePrepopulationFactory} capable of building a {@link Resource} from configuration
     */
    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resourceProvider", havingValue = "std", matchIfMissing = true)
    public StdResourcePrepopulationFactory resourcePrepopulationFactory() {
        return new StdResourcePrepopulationFactory();
    }

}
