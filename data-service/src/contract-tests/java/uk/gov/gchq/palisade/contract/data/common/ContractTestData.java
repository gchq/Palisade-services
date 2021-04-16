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
package uk.gov.gchq.palisade.contract.data.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.service.data.common.Context;
import uk.gov.gchq.palisade.service.data.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.data.common.resource.impl.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.data.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.data.common.rule.Rules;
import uk.gov.gchq.palisade.service.data.common.user.User;
import uk.gov.gchq.palisade.service.data.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.data.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.AuditableAuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.AuditableDataResponse;
import uk.gov.gchq.palisade.service.data.model.AuthorisedDataRequest;
import uk.gov.gchq.palisade.service.data.model.DataRequest;

import java.util.Collections;
import java.util.Map;

/**
 * Set of constants that are used in the testing
 */
public class ContractTestData {
    private static final ObjectMapper MAPPER = new ApplicationConfiguration().objectMapper();


    private ContractTestData() {
    }

    public static final String REQUEST_TOKEN = "test-request-token";
    public static final String LEAF_RESOURCE_ID = "file:/test/resource/file.txt";
    public static final DataRequest DATA_REQUEST = DataRequest.Builder.create()
            .withToken(REQUEST_TOKEN)
            .withLeafResourceId(LEAF_RESOURCE_ID);
    public static final JsonNode REQUEST_NODE;
    public static final String REQUEST_JSON = "{\"token\":\"test-request-token\",\"leafResourceId\":\"file:/test/resource/file.txt\"}";
    public static final DataRequest REQUEST_OBJ;

    static {
        try {
            REQUEST_NODE = MAPPER.readTree(REQUEST_JSON);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
        try {
            REQUEST_OBJ = MAPPER.treeToValue(REQUEST_NODE, DataRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    }

    public static final Context CONTEXT = new Context().purpose("testContext");
    public static final String USER_ID = "testUserId";
    public static final User USER = new User().userId(USER_ID);
    public static final String RESOURCE_ID = "test resource id";

    public static final LeafResource RESOURCE = new FileResource().id("/test/file.format")
            .type("java.lang.String")
            .serialisedFormat("format")
            .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
            .parent(new SystemResource().id("/test"));

    public static final Rules<LeafResource> RULES = new Rules<>();
    public static final Map<String, Object> ATTRIBUTES = Collections.singletonMap("test key", "test value");

    public static final AuditSuccessMessage AUDIT_SUCCESS_MESSAGE = AuditSuccessMessage.Builder.create()
            .withLeafResourceId(LEAF_RESOURCE_ID)
            .withUserId(USER_ID)
            .withResourceId(RESOURCE_ID)
            .withContext(CONTEXT)
            .withAttributes(ATTRIBUTES);

    public static final AuditErrorMessage AUDIT_ERROR_MESSAGE = AuditErrorMessage.Builder.create(DATA_REQUEST)
            .withAttributes(null)
            .withError(new ForbiddenException("Something went wrong!"));


    public static final AuthorisedDataRequest AUTHORISED_DATA = AuthorisedDataRequest.Builder.create()
            .withResource(RESOURCE)
            .withUser(USER)
            .withContext(CONTEXT)
            .withRules(RULES);

    public static final AuditableAuthorisedDataRequest AUDITABLE_DATA_REQUEST = AuditableAuthorisedDataRequest.Builder.create()
            .withDataRequest(DATA_REQUEST)
            .withAuthorisedData(AUTHORISED_DATA);

    public static final AuditableAuthorisedDataRequest AUDITABLE_DATA_REQUEST_WITH_ERROR = AuditableAuthorisedDataRequest.Builder.create()
            .withDataRequest(DATA_REQUEST)
            .withAuditErrorMessage(AUDIT_ERROR_MESSAGE);

    public static final AuditableDataResponse AUDITABLE_DATA_RESPONSE = AuditableDataResponse.Builder.create()
            .withToken(REQUEST_TOKEN)
            .withSuccessMessage(AUDIT_SUCCESS_MESSAGE)
            .withAuditErrorMessage(null);
}
