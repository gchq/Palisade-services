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

package uk.gov.gchq.palisade.service.resource.service;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.service.ConnectionDetail;

import java.io.IOException;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class HadoopResourceConfigurationService extends HadoopResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HadoopResourceConfigurationService.class);

    private HadoopResourceService resourceService;
    private Configuration configuration;

    public HadoopResourceConfigurationService(final Configuration configuration) throws IOException {
        this.configuration = configuration;
        resourceService = new HadoopResourceService(configuration);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByResource(final Resource resource) {
        return resourceService.getResourcesByResource(resource);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesById(final String resourceId) {
        return resourceService.getResourcesById(resourceId);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesByType(final String resourceType) {
        return resourceService.getResourcesByType(resourceType);
    }

    @Override
    public Map<LeafResource, ConnectionDetail> getResourcesBySerialisedFormat(final String resourceSerialisedFormat) {
        return resourceService.getResourcesBySerialisedFormat(resourceSerialisedFormat);
    }

    @Override
    public Resource addResource(final Resource resource) {
        LOGGER.debug("Adding Resource : {}", resource);
        LOGGER.info("Adding Resource: {}", resource.getId());
        requireNonNull(resource, "Request cannot be empty");

        return resource;
    }
}
