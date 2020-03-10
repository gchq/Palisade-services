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
package uk.gov.gchq.palisade.service.palisade.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.palisade.domain.DataRequestEntity;
import uk.gov.gchq.palisade.service.palisade.domain.LeafResourceRulesEntity;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import javax.transaction.Transactional;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static uk.gov.gchq.palisade.service.palisade.service.PalisadeService.TOKEN_NOT_FOUND_MESSAGE;

public class JpaPersistenceLayer implements PersistenceLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaPersistenceLayer.class);

    private final DataRequestRepository dataRequestRepository;
    private final LeafResourceRulesRepository leafResourceRulesRepository;
    private final Executor executor;

    public JpaPersistenceLayer(final DataRequestRepository dataRequestRepository, final LeafResourceRulesRepository leafResourceRulesRepository, final Executor executor) {
        this.dataRequestRepository = requireNonNull(dataRequestRepository, "DataRequestRepository");
        this.leafResourceRulesRepository = requireNonNull(leafResourceRulesRepository, "LeafResourceRulesRepository");
        this.executor = requireNonNull(executor, "Executor");
    }

    /**
     * Read the persisted data back into the domain storage objects and then convert them into the original application message types.
     *
     * @param requestId of the original request
     * @return the {@link DataRequestConfig} object for this request id
     */
    @Override
    public DataRequestConfig get(final String requestId) {
        final List<LeafResourceRulesEntity> leafResourceRules = this.leafResourceRulesRepository.getByRequestId(requestId);
        final DataRequestEntity dataRequest = this.dataRequestRepository.getByRequestId(requestId);

        dataRequest.setLeafResourceMap(leafResourceRules.stream().map(LeafResourceRulesEntity::leafResourceRules).collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue)));
        return dataRequest.dataRequestConfig();
    }

    @Override
    public CompletableFuture<DataRequestConfig> getAsync(final String requestId) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(this.get(requestId))
                .map(result -> {
                    LOGGER.debug("Got cache: {}", result);
                    return result;
                })
                .orElseThrow(() -> new RuntimeException(TOKEN_NOT_FOUND_MESSAGE + requestId)), this.executor);
    }

    /**
     * Convert the application message type to storage domain objects and write them to storage.
     *
     * @param dataRequestConfig message object to be persisted
     */
    @Override
    @Transactional
    public void put(final DataRequestConfig dataRequestConfig) {
        final DataRequestEntity dataRequest = new DataRequestEntity(dataRequestConfig);

        final List<LeafResourceRulesEntity> resources = dataRequestConfig.getRules().entrySet().stream()
                .map(entry -> new LeafResourceRulesEntity(dataRequestConfig.getOriginalRequestId(), entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        this.leafResourceRulesRepository.saveAll(resources);
        this.dataRequestRepository.save(dataRequest);
        LOGGER.debug("cached: {}", dataRequestConfig);
    }
}
