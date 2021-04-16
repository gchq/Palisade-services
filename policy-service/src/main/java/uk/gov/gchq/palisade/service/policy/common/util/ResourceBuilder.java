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

package uk.gov.gchq.palisade.service.policy.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.policy.common.resource.ConnectionDetail;
import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.ParentResource;
import uk.gov.gchq.palisade.service.policy.common.resource.Resource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.SystemResource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Provides a common set of utilities for constructing resources with all parents
 * automatically constructed recursively. This primarily targets filesystem-like
 * resources (Files, Directories etc.)
 * Internally, the resourceId is converted to a URI.
 * Can produce any of the following output types:
 * - {@link FileResource}
 * - {@link DirectoryResource}
 * - {@link SystemResource}
 * Any parents automatically constructed will also be from this collection.
 * If another method of creating a resource is required (i.e. directly using strings)
 * there is no guarantee that this can correctly resolve parents. Instead use the
 * methods provided by the appropriate resource impl.
 */
public class ResourceBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceBuilder.class);
    private static final URI ROOT;

    static {
        File userDir = new File(System.getProperty("user.dir"));
        URI root;
        try {
            root = userDir.getCanonicalFile().toURI();
        } catch (IOException ex) {
            LOGGER.error("ResourceBuilder threw an error when getting the CanonicalFile ", ex);
            root = userDir.getAbsoluteFile().toURI();
        }
        ROOT = root;
    }

    public ResourceBuilder() {
        // empty private constructor
    }

    private enum Scheme {
        FILE,
        HDFS
    }

    /**
     * Validates the uri, if the scheme is valid and not null a boolean true will be returned
     *
     * @param uri the uri to validate
     * @return a boolean value representing the validity of the uri
     */
    public static boolean canCreate(final URI uri) {
        try {
            Scheme.valueOf(uri.getScheme()); // Or throw
            return uri.isAbsolute();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Create a leafResource from a uri, connectionDetail, type, serialisedFormat and attribute map
     * Throw IllegalArgumentException if unsupported scheme
     * Throw ClassCastException if uri did not point to a leaf resource
     *
     * @param uri              the uri location of the resource
     * @param connectionDetail the service storing the resource
     * @param type             the type of resource
     * @param serialisedFormat the format of the resource e.g avro, txt
     * @param attributes       any additional attributes about the resource
     * @return a new LeafResource populated with these resources
     */
    public static LeafResource create(final URI uri, final ConnectionDetail connectionDetail, final String type, final String serialisedFormat, final Map<String, String> attributes) {
        return ((LeafResource) create(uri, attributes))
                .connectionDetail(connectionDetail)
                .type(type)
                .serialisedFormat(serialisedFormat);
    }

    /**
     * Create a resource from a uri and attribute map
     * Throw IllegalArgumentException if unsupported scheme
     *
     * @param uri        the id of the resource
     * @param attributes any additional attributes about the resource
     * @return a new resource created using this uri
     */
    public static Resource create(final URI uri, final Map<String, String> attributes) {
        // If passed relative paths, we can resolve them against the user.dir system property
        URI absolute;
        if (uri.isAbsolute()) {
            absolute = uri;
        } else {
            absolute = ROOT.resolve(uri);
        }
        // The hostname is all in the connectionDetail, so we never have a case of file://hostname/some/uri
        // A lot of this is trying to normalize file:///some/uri (file://<no-hostname>/some/uri) to file:/some/uri
        URI normal = UriBuilder.create(absolute)
                .withoutScheme()
                .withoutAuthority()
                .withoutPath()
                .withoutQuery()
                .withoutFragment();

        // This should be assigning the attributes map to the returned object, once resources support attribute maps

        switch (Scheme.valueOf(normal.getScheme().toUpperCase(Locale.ENGLISH))) {
            // Both file:/ and hdfs:/ schema produce filesystem-like structures
            case FILE:
            case HDFS:
                return filesystemSchema(normal);
            default:
                throw new IllegalArgumentException("No such implementation for uri scheme " + normal.getScheme());
        }
    }

    /**
     * Create a resource from a uri
     * Default to an empty attribute map
     * Throw IllegalArgumentException if unsupported scheme
     *
     * @param uri an id of a resource
     * @return a newly created resource with an empty map of attributes
     */
    public static Resource create(final URI uri) {
        return create(uri, Collections.emptyMap());
    }

    /**
     * Create a resource from a uri string and attribute map
     * Throw IllegalArgumentException if invalid uri string or unsupported scheme
     *
     * @param uriString  a string value of a url used to create a new resource
     * @param attributes any additional attributes about the resource
     * @return a newly created resource using these parameters
     */
    public static Resource create(final String uriString, final Map<String, String> attributes) {
        try {
            return create(new URI(uriString), attributes);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URISyntaxException converting string '" + uriString + "' to uri", ex);
        }
    }

    /**
     * Create a resource from a uri string
     * Default to an empty attribute map
     * Throw IllegalArgumentException if invalid uri string or unsupported scheme
     *
     * @param uriString a string value of a url used to create a new resource
     * @return a newly created resource with this url and an empty map of attributes
     */
    public static Resource create(final String uriString) {
        return create(uriString, Collections.emptyMap());
    }

    private static FileResource fileResource(final URI uri) {
        return new FileResource()
                .id(uri.normalize().toString())
                .parent((ParentResource) filesystemSchema(uri.resolve(".")));
    }

    private static DirectoryResource directoryResource(final URI uri) {
        return new DirectoryResource()
                .id(uri.normalize().toString())
                .parent((ParentResource) filesystemSchema(uri.resolve("..")));
    }

    private static SystemResource systemResource(final URI uri) {
        return new SystemResource()
                .id(uri.normalize().toString());
    }

    private static Resource filesystemSchema(final URI uri) {
        if (!uri.resolve(".").equals(uri)) {
            return fileResource(uri);
        } else if (Objects.nonNull(Path.of(uri).getParent())) {
            return directoryResource(uri);
        } else {
            return systemResource(uri);
        }
    }
}
