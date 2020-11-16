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

package uk.gov.gchq.palisade.service.audit.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.audit.model.AuditRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LoggerAuditServiceTest extends AuditServiceTestCommon {

    @Mock
    Logger logger;
    @Captor
    ArgumentCaptor<String> logCaptor;

    private static LoggerAuditService auditService;
    private UserId userId;
    private User user;
    private Context context;
    private RequestId requestId;
    private LeafResource resource;
    private Exception exception;
    private Rules rules;

    @Before
    public void setUp() {
        auditService = new LoggerAuditService(logger);

        userId = mockUserID();
        user = mockUser();
        context = mockContext();
        requestId = mockOriginalRequestId();
        resource = mockResource();
        exception = mockException();
        rules = mockRules();
    }



}
