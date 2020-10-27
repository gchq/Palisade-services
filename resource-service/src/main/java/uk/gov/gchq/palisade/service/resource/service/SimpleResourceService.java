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

package uk.gov.gchq.palisade.service.resource.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The Simple implementation of type {@link ResourceService} which extends {@link uk.gov.gchq.palisade.service.Service}
 */
public class SimpleResourceService implements ResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleResourceService.class);

    private final String dataServiceName;
    private final String resourceType;

    /**
     * Instantiates a new Simple resource service.
     *
     * @param dataServiceName the data service name used in the connection detail to contain the location,
     *                        either URL or hostname for the data service associated with this resource
     * @param resourceType    the type of resource returned by the service, a string representation of a
     *                        java class - the class itself does not need to be available to the service,
     *                        as it is only passed around as a String
     */
    public SimpleResourceService(final String dataServiceName, final String resourceType) {
        this.dataServiceName = dataServiceName;
        this.resourceType = resourceType;
    }

    private Stream<File> filesOf(final Path path) {
        try {
            return Files.walk(path)
                    .map(Path::toFile)
                    .map(file -> {
                        try {
                            return file.getCanonicalFile();
                        } catch (IOException e) {
                            LOGGER.warn("Failed to get canonical file", e);
                            return file.getAbsoluteFile();
                        }
                    })
                    .filter(File::isFile);
        } catch (IOException ex) {
            LOGGER.error("Could not walk {}", path);
            LOGGER.error("Error was: ", ex);
            return Stream.empty();
        }
    }

    private LeafResource asFileResource(final File file) {
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i + 1);
        }

        return ((FileResource) ResourceBuilder.create(file.toURI()))
                .serialisedFormat(extension)
                .type(this.resourceType)
                .connectionDetail(new SimpleConnectionDetail().serviceName(this.dataServiceName));
    }

    /**
     * Query returns a stream of {@link LeafResource} after walking the path of the uri passed in using a filter on the predicate.
     *
     * @param resourceId    the {@link String} value of the resourceId
     * @param pred          the predicate of {@link LeafResource}
     * @return the stream of {@link LeafResource}
     */
    protected LeafResource query(final String resourceId, final Predicate<LeafResource> pred) {
        return (LeafResource) ResourceBuilder.create(resourceId);
    }

    @Override
    public LeafResource getResourcesById(final String resourceId) {
        return query(resourceId, x -> true);
    }

    @Override
    public Boolean addResource(final LeafResource leafResource) {
        return false;
    }
}
