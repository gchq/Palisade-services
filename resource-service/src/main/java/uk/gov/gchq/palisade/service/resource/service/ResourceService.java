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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.resource.ChildResource;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.ServiceState;
import uk.gov.gchq.palisade.service.palisade.service.CacheService;
import uk.gov.gchq.palisade.service.resource.request.AddResourceRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByResourceRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesByTypeRequest;
import uk.gov.gchq.palisade.service.resource.request.GetResourcesBySerialisedFormatRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class ResourceService implements IResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);
    private static final String ERROR_ADD_RESOURCE = "AddResource is not supported by HadoopResourceService resources should be added/created via regular file system behaviour.";
    private static final String ERROR_OUT_SCOPE = "resource ID is out of scope of the this resource Service. Found: %s expected: %s";
    private static final String ERROR_RESOLVING_PARENTS = "Error occurred while resolving resourceParents";
    private static final String ERROR_NO_DATA_SERVICES = "No Hadoop data services known about in Hadoop resource service";

    private static final String HADOOP_CONF_STRING = "hadoop.init.conf";
    private static final String CACHE_IMPL_KEY = "hadoop.cache.svc";
    private static final String DATASERVICE_LIST = "hadoop.data.svc.list";

    private static final Pattern FILE_PAT = Pattern.compile("(?i)(?<=^file:)/(?=([^/]|$))");

    private Configuration conf;
    private CacheService cacheService;
    private FileSystem fileSystem;

    private List<ConnectionDetail> dataServices = new ArrayList<>();

    private String filename;

    public ResourceService() {
    }

    public ResourceService(final Configuration conf, final CacheService cacheService) throws IOException {
        requireNonNull(conf, "conf");
        requireNonNull(cacheService, "cache");
        this.conf = conf;
        this.fileSystem = FileSystem.get(conf);
        this.cacheService = cacheService;
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesByResource(GetResourcesByResourceRequest request) {
        LOGGER.debug("Invoking getResourcesByResource: {}", request);
        GetResourcesByIdRequest getResourcesByIdRequest = new GetResourcesByIdRequest().resourceId(request.getResource().getId());
        getResourcesByIdRequest.setOriginalRequestId(request.getOriginalRequestId());
        return getResourcesById(getResourcesByIdRequest);
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesById(GetResourcesByIdRequest request) {
        LOGGER.debug("Invoking getResourcesById: {}", request);
        String resourceId = request.getResourceId();
        final String path = getInternalConf().get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY);
        if (!resourceId.startsWith(path)) {
            throw new UnsupportedOperationException(java.lang.String.format(ERROR_OUT_SCOPE, resourceId, path));
        }
        return getMapCompletableFuture(resourceId, ignore -> true);
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesByType(GetResourcesByTypeRequest request) {
        LOGGER.debug("Invoking getResourcesByType: {}", request);
        final String pathString = getInternalConf().get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY);
        final Predicate<ResourceDetails> predicate = detail -> request.getType().equals(detail.getType());
        return getMapCompletableFuture(pathString, predicate);
    }

    @Override
    public CompletableFuture<Map<LeafResource, ConnectionDetail>> getResourcesBySerialisedFormat(GetResourcesBySerialisedFormatRequest request) {
        LOGGER.debug("Invoking getResourcesBySerialisedFormat: {}", request);
        final String pathString = getInternalConf().get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY);
        final Predicate<ResourceDetails> predicate = detail -> request.getSerialisedFormat().equals(detail.getFormat());
        return getMapCompletableFuture(pathString, predicate);
    }

    @Override
    public CompletableFuture<Boolean> addResource(AddResourceRequest request) {
        LOGGER.debug("Invoking addResource: {}", request);
        throw new UnsupportedOperationException(ERROR_ADD_RESOURCE);
    }

    private CompletableFuture<Map<LeafResource, ConnectionDetail>> getMapCompletableFuture(
            final String pathString, final Predicate<ResourceDetails> predicate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                //pull latest connection details
                final RemoteIterator<LocatedFileStatus> remoteIterator = this.getFileSystem().listFiles(new Path(pathString), true);
                return getPaths(remoteIterator)
                        .stream()
                        .map(ResourceDetails::getResourceDetailsFromFileName)
                        .filter(predicate)
                        .collect(Collectors.toMap(
                                resourceDetails -> {
                                    final String fileName = resourceDetails.getFileName();
                                    final FileResource fileFileResource = new FileResource().id(fileName).type(resourceDetails.getType()).serialisedFormat(resourceDetails.getFormat());
                                    resolveParents(fileFileResource, getInternalConf());
                                    return fileFileResource;
                                },
                                resourceDetails -> {
                                    if (this.dataServices.size() < 1) {
                                        throw new IllegalStateException(ERROR_NO_DATA_SERVICES);
                                    }
                                    int service = ThreadLocalRandom.current().nextInt(this.dataServices.size());
                                    return this.dataServices.get(service);
                                }
                                )
                        );
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void resolveParents(final ChildResource resource, final Configuration configuration) {
        try {
            final String connectionDetail = resource.getId();
            final Path path = new Path(connectionDetail);
            final int fileDepth = path.depth();
            final int fsDepth = new Path(configuration.get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY)).depth();

            if (fileDepth > fsDepth + 1) {
                DirectoryResource parent = new DirectoryResource().id(fixURIScheme(path.getParent().toString()));
                resource.setParent(parent);
                resolveParents(parent, configuration);
            } else {
                resource.setParent(new SystemResource().id(fixURIScheme(path.getParent().toString())));
            }
        } catch (Exception e) {
            throw new RuntimeException(ERROR_RESOLVING_PARENTS, e);
        }
    }

    /**
     * Fixes URI schemes that use the file: scheme with no authority component. Any URI that starts file:/ will be changed to file:///
     *
     * @param uri URI to check
     * @return the file URI with authority separator inserted
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    private static String fixURIScheme(final String uri) {
        requireNonNull(uri, "uri");
        Matcher match = FILE_PAT.matcher(uri);
        if (match.find()) {
            return match.replaceFirst("///");
        } else {
            return uri;
        }
    }

    private Map<String, String> getPlainJobConfWithoutResolvingValues() {
        Map<String, String> plainMapWithoutResolvingValues = new HashMap<>();
        for (Map.Entry<String, String> entry : new Configuration()) {
            plainMapWithoutResolvingValues.put(entry.getKey(), entry.getValue());
        }
        return plainMapWithoutResolvingValues;
    }

    protected Configuration getInternalConf() {
        requireNonNull(conf, "configuration must be set");
        return conf;
    }

    protected FileSystem getFileSystem() {
        requireNonNull(fileSystem, "configuration must be set");
        return fileSystem;
    }

    protected static Collection<String> getPaths(final RemoteIterator<LocatedFileStatus> remoteIterator) throws IOException {
        final ArrayList<String> paths = Lists.newArrayList();
        while (remoteIterator.hasNext()) {
            final LocatedFileStatus next = remoteIterator.next();
            final String pathWithoutFSName = next.getPath().toUri().toString();
            paths.add(pathWithoutFSName);
        }
        return paths;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
    public Map<String, String> getConf() {
        Map<String, String> rtn = Maps.newHashMap();
        Map<String, String> plainJobConfWithoutResolvingValues = getPlainJobConfWithoutResolvingValues();

        for (Map.Entry<String, String> entry : getInternalConf()) {
            final String plainValue = plainJobConfWithoutResolvingValues.get(entry.getKey());
            final String thisValue = entry.getValue();
            if (isNull(plainValue) || !plainValue.equals(thisValue)) {
                rtn.put(entry.getKey(), entry.getValue());
            }
        }
        return rtn;
    }

    @Override
    public void recordCurrentConfigTo(final ServiceState config) {
        requireNonNull(config, "config");
        config.put(ResourceService.class.getTypeName(), getClass().getTypeName());
        Map<String, String> confMap = getConf();
        String serialisedConf = new String(JSONSerialiser.serialise(confMap), StandardCharsets.UTF_8);
        config.put(HADOOP_CONF_STRING, serialisedConf);
        String serialisedCache = new String(JSONSerialiser.serialise(cacheService), StandardCharsets.UTF_8);
        config.put(CACHE_IMPL_KEY, serialisedCache);
        String serialisedDataServices = new String(JSONSerialiser.serialise(dataServices), StandardCharsets.UTF_8);
        config.put(DATASERVICE_LIST, serialisedDataServices);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ResourceService that = (ResourceService) o;

        final EqualsBuilder builder = new EqualsBuilder()
                .append(this.getFileSystem(), that.getFileSystem())
                .append(this.cacheService, that.cacheService);

        if (builder.isEquals()) {
            builder.append(this.getInternalConf().size(), that.getInternalConf().size());
            for (Map.Entry<String, String> entry : this.getInternalConf()) {
                final String lhs = this.getInternalConf().get(entry.getKey());
                final String rhs = that.getInternalConf().get(entry.getKey());
                builder.append(lhs, rhs);
                if (!builder.isEquals()) {
                    break;
                }
            }
        }

        return builder.isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("conf", conf)
                .append("fileSystem", fileSystem)
                .append("cacheService", cacheService)
                .toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(conf)
                .append(fileSystem)
                .append(cacheService)
                .toHashCode();
    }
}
