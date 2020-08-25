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

package uk.gov.gchq.palisade.service.queryscope.web;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.service.queryscope.QueryScopeApplicationTestData;
import uk.gov.gchq.palisade.service.queryscope.request.StreamMarker;
import uk.gov.gchq.palisade.service.queryscope.service.AuditService;
import uk.gov.gchq.palisade.service.queryscope.service.QueryScopeService;

import static org.mockito.ArgumentMatchers.any;

class QueryScopeControllerTest {

    QueryScopeService mockQueryScopeService = Mockito.mock(QueryScopeService.class);
    AuditService mockAuditService = Mockito.mock(AuditService.class);

    @Test
    void queryScopeServiceDelegatesToPersistenceLayer() {
        // given
        QueryScopeController queryScopeController = new QueryScopeController(mockQueryScopeService, mockAuditService);

        // when
        queryScopeController.storeRequestResult(
                null,
                QueryScopeApplicationTestData.REQUEST_TOKEN,
                QueryScopeApplicationTestData.REQUEST
        );

        // then
        Mockito.verify(mockQueryScopeService, Mockito.atLeastOnce()).storeRequestResult(
                QueryScopeApplicationTestData.REQUEST_TOKEN,
                QueryScopeApplicationTestData.USER,
                QueryScopeApplicationTestData.LEAF_RESOURCE,
                QueryScopeApplicationTestData.CONTEXT,
                QueryScopeApplicationTestData.RULES
        );
    }

    @Test
    void streamMarkerBypassesServiceDelegation() {
        // given
        QueryScopeController queryScopeController = new QueryScopeController(mockQueryScopeService, mockAuditService);

        // when
        queryScopeController.storeRequestResult(
                StreamMarker.START_OF_STREAM,
                QueryScopeApplicationTestData.REQUEST_TOKEN,
                QueryScopeApplicationTestData.REQUEST
        );

        // then
        Mockito.verify(mockQueryScopeService, Mockito.never()).storeRequestResult(
                any(),
                any(),
                any(),
                any(),
                any()
        );

        // when
        queryScopeController.storeRequestResult(
                StreamMarker.END_OF_STREAM,
                QueryScopeApplicationTestData.REQUEST_TOKEN,
                QueryScopeApplicationTestData.REQUEST
        );

        // then
        Mockito.verify(mockQueryScopeService, Mockito.never()).storeRequestResult(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

}