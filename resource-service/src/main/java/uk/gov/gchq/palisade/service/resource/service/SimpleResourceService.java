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
import uk.gov.gchq.palisade.service.resource.config.ClientConfiguration;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SimpleResourceService implements ResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleResourceService.class);

    private final ClientConfiguration clientConfiguration;

    public SimpleResourceService(final ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    private Stream<File> filesOf(final Path path) {
        try {
            return Files.walk(path)
                    .map(Path::toFile)
                    .map(file -> {
                        try {
                            return file.getCanonicalFile();
                        } catch (IOException e) {
                            LOGGER.error("Failed to get canonical file", e);
                            return file;
                        }
                    })
                    .filter(File::isFile);
        } catch (Exception ex) {
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
        URI dataServiceUri = clientConfiguration.getClientUri("data-service").orElseThrow(() -> new RuntimeException("Failed to find any instance of 'data-service'"));
        return ((FileResource) ResourceBuilder.create(file.toURI()))
                .serialisedFormat(extension)
                .type("txt")
                .connectionDetail(new SimpleConnectionDetail().uri(dataServiceUri.toString()));
    }

    protected Stream<LeafResource> query(final URI uri, final Predicate<LeafResource> pred) {
        return filesOf(Path.of(uri))
                .map(this::asFileResource)
                .filter(pred);
    }

    private URI stringToURI(final String uriString) {
        try {
            return new URI(uriString);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URISyntaxException converting '" + uriString + "' to URI", ex);
        }
    }

    private URI filesystemURI(final String fileString) {
        try {
            return new File(fileString).getCanonicalFile().toURI();
        } catch (IOException ex) {
            return new File(fileString).getAbsoluteFile().toURI();
        }
    }

    @Override
    public Stream<LeafResource> getResourcesByResource(final Resource resource) {
        return query(stringToURI(resource.getId()), x -> true);
    }

    @Override
    public Stream<LeafResource> getResourcesById(final String resourceId) {
        return query(stringToURI(resourceId), x -> true);
    }

    @Override
    public Stream<LeafResource> getResourcesByType(final String type) {
        return query(filesystemURI(System.getProperty("user.dir")), leafResource -> leafResource.getType().equals(type));
    }

    @Override
    public Stream<LeafResource> getResourcesBySerialisedFormat(final String serialisedFormat) {
        return query(filesystemURI(System.getProperty("user.dir")), leafResource -> leafResource.getSerialisedFormat().equals(serialisedFormat));
    }

    @Override
    public Boolean addResource(final LeafResource leafResource) {
        return false;
    }
}
