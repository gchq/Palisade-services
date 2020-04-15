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
import uk.gov.gchq.palisade.resource.ParentResource;
import uk.gov.gchq.palisade.resource.Resource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SimpleResourceService implements ResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleResourceService.class);

    private Stream<File> filesOf(final Path path) {
        try {
            return Files.walk(path)
                    .map(Path::toAbsolutePath)
                    .map(Path::toFile)
                    .filter(File::isFile);
        } catch (Exception ex) {
            LOGGER.error("Could not walk {}", path);
            LOGGER.error("Error was: ", ex);
            return Stream.empty();
        }
    }

    private ParentResource asDirectoryResource(final Path directory) {
        if (Objects.isNull(directory.getParent())) {
            return new SystemResource().id(directory.toString());
        } else {
            return new DirectoryResource().id(directory.toString()).parent(asDirectoryResource(directory.getParent()));
        }
    }

    private LeafResource asLeafResource(final File file) {
        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i + 1);
        }
        Path path = file.toPath();
        return new FileResource()
                .id(path.toString())
                .type(extension)
                .serialisedFormat("txt")
                .connectionDetail(new SimpleConnectionDetail().uri("localhost"))
                .parent(asDirectoryResource(path.getParent()));
    }

    Stream<LeafResource> query(final String path, final Predicate<LeafResource> pred) {
        return filesOf(Path.of(path))
                .map(this::asLeafResource)
                .filter(pred);
    }

    @Override
    public Stream<LeafResource> getResourcesByResource(final Resource resource) {
        return query(resource.getId(), x -> true);
    }

    @Override
    public Stream<LeafResource> getResourcesById(final String resourceId) {
        return query(resourceId, x -> true);
    }

    @Override
    public Stream<LeafResource> getResourcesByType(final String type) {
        return query(".", leafResource -> leafResource.getType().equals(type));
    }

    @Override
    public Stream<LeafResource> getResourcesBySerialisedFormat(final String serialisedFormat) {
        return query(".", leafResource -> leafResource.getSerialisedFormat().equals(serialisedFormat));
    }

    @Override
    public Boolean addResource(final LeafResource leafResource) {
        return false;
    }
}
