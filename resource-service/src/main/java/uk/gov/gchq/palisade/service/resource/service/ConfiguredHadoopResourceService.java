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

package uk.gov.gchq.palisade.service.resource.service;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;

import java.io.IOException;

/**
 * Extend a Hadoop Resource Service by making it aware of all data-services available to it through
 * the connectionDetail of added leafResources.
 */
public class ConfiguredHadoopResourceService extends HadoopResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguredHadoopResourceService.class);

    /**
     * Create a {@link HadoopResourceService} using the supplied hadoop {@link Configuration}
     *
     * @param configuration an apache hadoop configuration
     * @throws IOException if the filesystem specified is unavailable
     */
    public ConfiguredHadoopResourceService(final Configuration configuration) throws IOException {
        super(configuration);
    }

    /**
     * Add a {@link LeafResource} to the service, also adding the leaf's connection detail to the service
     *
     * @param leafResource the resource that Palisade can manage access to
     * @return true if the leafResource was added successfully
     */
    @Override
    public Boolean addResource(final LeafResource leafResource) {
        LOGGER.info("Adding connectionDetail {} for leafResource {}", leafResource.getConnectionDetail(), leafResource);
        this.addDataService(leafResource.getConnectionDetail());
        return true;
    }
}
