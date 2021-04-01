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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.reader.common.ResourceService;
import uk.gov.gchq.palisade.reader.common.Service;
import uk.gov.gchq.palisade.reader.common.SimpleConnectionDetail;
import uk.gov.gchq.palisade.reader.common.resource.LeafResource;
import uk.gov.gchq.palisade.reader.common.resource.Resource;
import uk.gov.gchq.palisade.reader.common.util.ResourceBuilder;
import uk.gov.gchq.palisade.service.resource.exception.NoSuchResourceException;
import uk.gov.gchq.palisade.service.resource.service.FunctionalIterator.PlainIterator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The Simple implementation of type {@link ResourceService} which extends {@link Service}
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

    private static FunctionalIterator<File> filesOf(final Path path) {
        try {
            Stream<Path> filesWalk = Files.walk(path);
            return new StreamClosingIterator<>(filesWalk)
                    .map(Path::toFile)
                    .map((File file) -> {
                        try {
                            return file.getCanonicalFile();
                        } catch (IOException e) {
                            LOGGER.warn("Failed to get canonical file", e);
                            return file.getAbsoluteFile();
                        }
                    })
                    .filter(File::isFile);
        } catch (IOException ex) {
            throw new NoSuchResourceException("Failed to walk path " + path, ex);
        }
    }

    private LeafResource asFileResource(final File file) {
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i + 1);
        }

        return ((LeafResource) ResourceBuilder.create(file.toURI()))
                .serialisedFormat(extension)
                .type(this.resourceType)
                .connectionDetail(new SimpleConnectionDetail().serviceName(this.dataServiceName));
    }

    /**
     * Query returns a stream of {@link LeafResource} after walking the path of the uri passed in using a filter on the predicate.
     *
     * @param uri  the uri converted from a String
     * @param pred the predicate of {@link LeafResource}
     * @return the stream of {@link LeafResource}
     */
    protected Iterator<LeafResource> query(final URI uri, final Predicate<LeafResource> pred) {
        return filesOf(Path.of(uri))
                .map(this::asFileResource)
                .filter(pred);
    }

    private static URI stringToURI(final String uriString) {
        try {
            return new URI(uriString);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URISyntaxException converting '" + uriString + "' to URI", ex);
        }
    }

    private static URI filesystemURI(final String fileString) {
        try {
            return new File(fileString).getCanonicalFile().toURI();
        } catch (IOException ex) {
            return new File(fileString).getAbsoluteFile().toURI();
        }
    }

    @Override
    public Iterator<LeafResource> getResourcesByResource(final Resource resource) {
        return query(stringToURI(resource.getId()), x -> true);
    }

    @Override
    public Iterator<LeafResource> getResourcesById(final String resourceId) {
        return query(stringToURI(resourceId), x -> true);
    }

    @Override
    public Iterator<LeafResource> getResourcesByType(final String type) {
        return query(filesystemURI(System.getProperty("user.dir")), leafResource -> leafResource.getType().equals(type));
    }

    @Override
    public Iterator<LeafResource> getResourcesBySerialisedFormat(final String serialisedFormat) {
        return query(filesystemURI(System.getProperty("user.dir")), leafResource -> leafResource.getSerialisedFormat().equals(serialisedFormat));
    }

    @Override
    public Boolean addResource(final LeafResource leafResource) {
        return false;
    }

    /**
     * A {@link FunctionalIterator} implementation wrapping a stream.
     * Avoid using unless essential, which in this case is because of {@link Files#walk}.
     *
     * @param <T> iterator and stream type
     */
    private static class StreamClosingIterator<T> extends PlainIterator<T> {

        private final Stream<T> closeableStream;

        StreamClosingIterator(final Stream<T> stream) {
            super(stream.iterator());
            this.closeableStream = stream;
        }

        @Override
        public void close() {
            this.closeableStream.close();
        }
    }
}
