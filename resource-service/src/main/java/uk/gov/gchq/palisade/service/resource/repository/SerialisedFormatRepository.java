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
package uk.gov.gchq.palisade.service.resource.repository;

import org.springframework.data.repository.CrudRepository;

import uk.gov.gchq.palisade.service.resource.domain.SerialisedFormatEntity;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Low-level requirement for a database used for persistence, see {@link SerialisedFormatEntity}
 * for more details
 */
public interface SerialisedFormatRepository extends CrudRepository<SerialisedFormatEntity, String> {

    default Stream<SerialisedFormatEntity> streamFindAllBySerialisedFormat(String serialisedFormat) {
        return StreamSupport.stream(findAllBySerialisedFormat(serialisedFormat).spliterator(), false);
    }

    Iterable<SerialisedFormatEntity> findAllBySerialisedFormat(String serialisedFormat);

}
