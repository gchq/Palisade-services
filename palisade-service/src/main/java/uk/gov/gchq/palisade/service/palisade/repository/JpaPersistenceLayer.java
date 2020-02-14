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

import uk.gov.gchq.palisade.service.palisade.domain.DataRequestEntity;
import uk.gov.gchq.palisade.service.palisade.domain.LeafResourceRulesEntity;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import javax.transaction.Transactional;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.stream.Collectors;

public class JpaPersistenceLayer implements PersistenceLayer {

    private DataRequestRepository dataRequestRepository;
    private LeafResourceRulesRepository leafResourceRulesRepository;

    public JpaPersistenceLayer(final DataRequestRepository dataRequestRepository, final LeafResourceRulesRepository leafResourceRulesRepository) {
        this.dataRequestRepository = dataRequestRepository;
        this.leafResourceRulesRepository = leafResourceRulesRepository;
    }

    @Override
    public DataRequestConfig get(final String requestId) {
        final List<LeafResourceRulesEntity> leafResourceRules = this.leafResourceRulesRepository.getByRequestId(requestId);
        final DataRequestEntity dataRequest = this.dataRequestRepository.getByRequestId(requestId);

        dataRequest.setLeafResourceMap(leafResourceRules.stream().map(LeafResourceRulesEntity::leafResourceRules).collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue)));
        return dataRequest.dataRequestConfig();
    }

    @Override
    @Transactional
    public void put(final DataRequestConfig dataRequestConfig) {
        final DataRequestEntity dataRequest = new DataRequestEntity(dataRequestConfig);

        final List<LeafResourceRulesEntity> resources = dataRequestConfig.getRules().entrySet().stream()
                                                            .map(entry -> new LeafResourceRulesEntity(dataRequestConfig.getId(), entry.getKey(), entry.getValue()))
                                                                .collect(Collectors.toList());

        this.leafResourceRulesRepository.saveAll(resources);
        this.dataRequestRepository.save(dataRequest);
    }
}
