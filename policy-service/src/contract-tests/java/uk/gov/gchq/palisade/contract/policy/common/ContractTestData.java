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

package uk.gov.gchq.palisade.contract.policy.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.serializer.support.SerializationFailedException;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.service.policy.exception.NoSuchPolicyException;
import uk.gov.gchq.palisade.service.policy.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.policy.model.PolicyRequest;
import uk.gov.gchq.palisade.service.policy.model.Token;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Common test data for all classes
 * This cements the expected JSON input and output, providing an external contract for the service
 */
public class ContractTestData {

    private ContractTestData() {
        // hide the constructor, this is just a collection of static objects
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final JsonNode REQUEST_NODE;
    public static final JsonNode NO_RESOURCE_RULES_REQUEST_NODE;
    public static final JsonNode REDACTED_RESOURCE_RULES_REQUEST_NODE;
    public static final PolicyRequest REQUEST_OBJ;
    public static final PolicyRequest NO_RESOURCE_RULES_REQUEST_OBJ;
    public static final PolicyRequest REDACTED_RESOURCE_RULES_REQUEST_OBJ;
    public static final String REQUEST_JSON = "{\"userId\":\"test-user-id\",\"resourceId\":\"file:/test/resourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"test-purpose\"}},\"user\":{\"userId\":{\"id\":\"test-user-id\"},\"roles\":[\"role\"],\"auths\":[\"auth\"],\"class\":\"uk.gov.gchq.palisade.User\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"file:/test/resourceId\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-data-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"file:/test/\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"file:/\"}},\"serialisedFormat\":\"txt\",\"type\":\"test\"}}";
    public static final String NO_RESOURCE_RULES_REQUEST_JSON = "{\"userId\":\"noResourceRulesUser\",\"resourceId\":\"file:/test/noRulesResource\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"test-purpose\"}},\"user\":{\"userId\":{\"id\":\"noResourceRulesUser\"},\"roles\":[\"role\"],\"auths\":[\"auth\"],\"class\":\"uk.gov.gchq.palisade.User\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"file:/test/noRulesResource\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-data-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"file:/test/\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"file:/\"}},\"serialisedFormat\":\"txt\",\"type\":\"test\"}}";
    public static final String REDACTED_RESOURCE_RULES_REQUEST_JSON = "{\"userId\":\"redactedResourceRulesUser\",\"resourceId\":\"/test/recordRulesResource\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"test-purpose\"}},\"user\":{\"userId\":{\"id\":\"redactedResourceRulesUser\"},\"roles\":[\"role\"],\"auths\":[\"auth\"],\"class\":\"uk.gov.gchq.palisade.User\"},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"file:/test/recordRulesResource\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-data-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.DirectoryResource\",\"id\":\"file:/test/\",\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"file:/\"}},\"serialisedFormat\":\"txt\",\"type\":\"test\"}}";
    public static final String NO_SUCH_POLICY_JSON ="";
//{"userId":"originalUserID","resourceId":"testResourceId","context":{"class":"uk.gov.gchq.palisade.Context","contents":{"purpose":"testContext"}},"attributes":{"messagesSent":"23"},"error":{"cause":null,"stackTrace":[{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"testGroupedDependantErrorMessageSerialisingAndDeserialising","fileName":"AuditErrorMessageTest.java","lineNumber":63,"className":"uk.gov.gchq.palisade.component.policy.model.AuditErrorMessageTest","nativeMethod":false},{"classLoaderName":null,"moduleName":"java.base","moduleVersion":"11.0.4","methodName":"invoke0","fileName":"NativeMethodAccessorImpl.java","lineNumber":-2,"className":"jdk.internal.reflect.NativeMethodAccessorImpl","nativeMethod":true},{"classLoaderName":null,"moduleName":"java.base","moduleVersion":"11.0.4","methodName":"invoke","fileName":"NativeMethodAccessorImpl.java","lineNumber":62,"className":"jdk.internal.reflect.NativeMethodAccessorImpl","nativeMethod":false},{"classLoaderName":null,"moduleName":"java.base","moduleVersion":"11.0.4","methodName":"invoke","fileName":"DelegatingMethodAccessorImpl.java","lineNumber":43,"className":"jdk.internal.reflect.DelegatingMethodAccessorImpl","nativeMethod":false},{"classLoaderName":null,"moduleName":"java.base","moduleVersion":"11.0.4","methodName":"invoke","fileName":"Method.java","lineNumber":566,"className":"java.lang.reflect.Method","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"invokeMethod","fileName":"ReflectionUtils.java","lineNumber":686,"className":"org.junit.platform.commons.util.ReflectionUtils","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"proceed","fileName":"MethodInvocation.java","lineNumber":60,"className":"org.junit.jupiter.engine.execution.MethodInvocation","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"proceed","fileName":"InvocationInterceptorChain.java","lineNumber":131,"className":"org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"intercept","fileName":"TimeoutExtension.java","lineNumber":149,"className":"org.junit.jupiter.engine.extension.TimeoutExtension","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"interceptTestableMethod","fileName":"TimeoutExtension.java","lineNumber":140,"className":"org.junit.jupiter.engine.extension.TimeoutExtension","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"interceptTestMethod","fileName":"TimeoutExtension.java","lineNumber":84,"className":"org.junit.jupiter.engine.extension.TimeoutExtension","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$ofVoidMethod$0","fileName":"ExecutableInvoker.java","lineNumber":115,"className":"org.junit.jupiter.engine.execution.ExecutableInvoker$ReflectiveInterceptorCall","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$invoke$0","fileName":"ExecutableInvoker.java","lineNumber":105,"className":"org.junit.jupiter.engine.execution.ExecutableInvoker","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"proceed","fileName":"InvocationInterceptorChain.java","lineNumber":106,"className":"org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"proceed","fileName":"InvocationInterceptorChain.java","lineNumber":64,"className":"org.junit.jupiter.engine.execution.InvocationInterceptorChain","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"chainAndInvoke","fileName":"InvocationInterceptorChain.java","lineNumber":45,"className":"org.junit.jupiter.engine.execution.InvocationInterceptorChain","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"invoke","fileName":"InvocationInterceptorChain.java","lineNumber":37,"className":"org.junit.jupiter.engine.execution.InvocationInterceptorChain","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"invoke","fileName":"ExecutableInvoker.java","lineNumber":104,"className":"org.junit.jupiter.engine.execution.ExecutableInvoker","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"invoke","fileName":"ExecutableInvoker.java","lineNumber":98,"className":"org.junit.jupiter.engine.execution.ExecutableInvoker","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$invokeTestMethod$6","fileName":"TestMethodTestDescriptor.java","lineNumber":212,"className":"org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"ThrowableCollector.java","lineNumber":73,"className":"org.junit.platform.engine.support.hierarchical.ThrowableCollector","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"invokeTestMethod","fileName":"TestMethodTestDescriptor.java","lineNumber":208,"className":"org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"TestMethodTestDescriptor.java","lineNumber":137,"className":"org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"TestMethodTestDescriptor.java","lineNumber":71,"className":"org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$5","fileName":"NodeTestTask.java","lineNumber":135,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"ThrowableCollector.java","lineNumber":73,"className":"org.junit.platform.engine.support.hierarchical.ThrowableCollector","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$7","fileName":"NodeTestTask.java","lineNumber":125,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"around","fileName":"Node.java","lineNumber":135,"className":"org.junit.platform.engine.support.hierarchical.Node","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$8","fileName":"NodeTestTask.java","lineNumber":123,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"ThrowableCollector.java","lineNumber":73,"className":"org.junit.platform.engine.support.hierarchical.ThrowableCollector","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"executeRecursively","fileName":"NodeTestTask.java","lineNumber":122,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"NodeTestTask.java","lineNumber":80,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":null,"moduleName":"java.base","moduleVersion":"11.0.4","methodName":"forEach","fileName":"ArrayList.java","lineNumber":1540,"className":"java.util.ArrayList","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"invokeAll","fileName":"SameThreadHierarchicalTestExecutorService.java","lineNumber":38,"className":"org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$5","fileName":"NodeTestTask.java","lineNumber":139,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"ThrowableCollector.java","lineNumber":73,"className":"org.junit.platform.engine.support.hierarchical.ThrowableCollector","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$7","fileName":"NodeTestTask.java","lineNumber":125,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"around","fileName":"Node.java","lineNumber":135,"className":"org.junit.platform.engine.support.hierarchical.Node","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$8","fileName":"NodeTestTask.java","lineNumber":123,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"ThrowableCollector.java","lineNumber":73,"className":"org.junit.platform.engine.support.hierarchical.ThrowableCollector","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"executeRecursively","fileName":"NodeTestTask.java","lineNumber":122,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"NodeTestTask.java","lineNumber":80,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":null,"moduleName":"java.base","moduleVersion":"11.0.4","methodName":"forEach","fileName":"ArrayList.java","lineNumber":1540,"className":"java.util.ArrayList","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"invokeAll","fileName":"SameThreadHierarchicalTestExecutorService.java","lineNumber":38,"className":"org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$5","fileName":"NodeTestTask.java","lineNumber":139,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"ThrowableCollector.java","lineNumber":73,"className":"org.junit.platform.engine.support.hierarchical.ThrowableCollector","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$7","fileName":"NodeTestTask.java","lineNumber":125,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"around","fileName":"Node.java","lineNumber":135,"className":"org.junit.platform.engine.support.hierarchical.Node","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$executeRecursively$8","fileName":"NodeTestTask.java","lineNumber":123,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"ThrowableCollector.java","lineNumber":73,"className":"org.junit.platform.engine.support.hierarchical.ThrowableCollector","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"executeRecursively","fileName":"NodeTestTask.java","lineNumber":122,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"NodeTestTask.java","lineNumber":80,"className":"org.junit.platform.engine.support.hierarchical.NodeTestTask","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"submit","fileName":"SameThreadHierarchicalTestExecutorService.java","lineNumber":32,"className":"org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"HierarchicalTestExecutor.java","lineNumber":57,"className":"org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"HierarchicalTestEngine.java","lineNumber":51,"className":"org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"DefaultLauncher.java","lineNumber":248,"className":"org.junit.platform.launcher.core.DefaultLauncher","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"lambda$execute$5","fileName":"DefaultLauncher.java","lineNumber":211,"className":"org.junit.platform.launcher.core.DefaultLauncher","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"withInterceptedStreams","fileName":"DefaultLauncher.java","lineNumber":226,"className":"org.junit.platform.launcher.core.DefaultLauncher","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"DefaultLauncher.java","lineNumber":199,"className":"org.junit.platform.launcher.core.DefaultLauncher","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"execute","fileName":"DefaultLauncher.java","lineNumber":132,"className":"org.junit.platform.launcher.core.DefaultLauncher","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"startRunnerWithArgs","fileName":"JUnit5IdeaTestRunner.java","lineNumber":71,"className":"com.intellij.junit5.JUnit5IdeaTestRunner","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"startRunnerWithArgs","fileName":"IdeaTestRunner.java","lineNumber":33,"className":"com.intellij.rt.junit.IdeaTestRunner$Repeater","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"prepareStreamsAndStart","fileName":"JUnitStarter.java","lineNumber":220,"className":"com.intellij.rt.junit.JUnitStarter","nativeMethod":false},{"classLoaderName":"app","moduleName":null,"moduleVersion":null,"methodName":"main","fileName":"JUnitStarter.java","lineNumber":53,"className":"com.intellij.rt.junit.JUnitStarter","nativeMethod":false}],"message":"Something went wrong!","suppressed":[],"localizedMessage":"Something went wrong!"},"serverHostName":"CIC00948.lan","serviceName":"policy-service","timestamp":"2020-12-07T15:04:38.844594Z","serverIP":"192.168.2.228","serverHostname":"CIC00948.lan"}
    public static class PassThroughRule<T extends Serializable> implements Rule<T> {
        @Override
        public T apply(final T record, final User user, final Context context) {
            return record;
        }
    }

    public static final UserId USER_ID = new UserId().id("test-user-id");
    public static final String RESOURCE_ID = "/test/resourceId";
    public static final String PURPOSE = "test-purpose";
    public static final Context CONTEXT = new Context().purpose(PURPOSE);
    public static final NoSuchPolicyException NO_SUCH_POLICY_EXCEPTION = new NoSuchPolicyException("Test no such policy exception");
   // JsonContent<AuditErrorMessage> auditErrorMessageJsonContent = jsonTester.write(NO_SUCH_POLICY_EXCEPTION);
   // private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        try {
            REQUEST_NODE = MAPPER.readTree(REQUEST_JSON);
            NO_RESOURCE_RULES_REQUEST_NODE = MAPPER.readTree(NO_RESOURCE_RULES_REQUEST_JSON);
            REDACTED_RESOURCE_RULES_REQUEST_NODE = MAPPER.readTree(REDACTED_RESOURCE_RULES_REQUEST_JSON);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
        try {
            REQUEST_OBJ = MAPPER.treeToValue(REQUEST_NODE, PolicyRequest.class);
            NO_RESOURCE_RULES_REQUEST_OBJ = MAPPER.treeToValue(NO_RESOURCE_RULES_REQUEST_NODE, PolicyRequest.class);
            REDACTED_RESOURCE_RULES_REQUEST_OBJ = MAPPER.treeToValue(REDACTED_RESOURCE_RULES_REQUEST_NODE, PolicyRequest.class);
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to convert contract test data to objects", e);
        }
    }

    public static final Function<Integer, String> REQUEST_FACTORY_JSON = i -> String.format(REQUEST_JSON, i, i);
    public static final Function<Integer, String> NO_RESOURCE_RULES_REQUEST_FACTORY_JSON = i -> String.format(NO_RESOURCE_RULES_REQUEST_JSON, i, i);
    public static final Function<Integer, String> REDACTED_RESOURCE_RULES_REQUEST_FACTORY_JSON = i -> String.format(REDACTED_RESOURCE_RULES_REQUEST_JSON, i, i);
    public static final Function<Integer, String> NO_SUCH_POLICY_FACTORY_JSON = i -> String.format(NO_SUCH_POLICY_JSON, i, i);



    public static final Function<Integer, JsonNode> REQUEST_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };
    public static final Function<Integer, JsonNode> NO_RESOURCE_RULES_REQUEST_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(NO_RESOURCE_RULES_REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };
    public static final Function<Integer, JsonNode> REDACTED_RESOURCE_RULES_REQUEST_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(REDACTED_RESOURCE_RULES_REQUEST_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };

    public static final Function<Integer, JsonNode> NO_SUCH_POLICY_NODE_FACTORY_NODE = i -> {
        try {
            return MAPPER.readTree(NO_SUCH_POLICY_FACTORY_JSON.apply(i));
        } catch (JsonProcessingException e) {
            throw new SerializationFailedException("Failed to parse contract test data", e);
        }
    };

    public static final String REQUEST_TOKEN = "test-request-token";

    public static final Headers START_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes()), new RecordHeader(StreamMarker.HEADER, StreamMarker.START.toString().getBytes())});
    public static final Headers REQUEST_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes())});
    public static final Headers END_HEADERS = new RecordHeaders(new Header[]{new RecordHeader(Token.HEADER, REQUEST_TOKEN.getBytes()), new RecordHeader(StreamMarker.HEADER, StreamMarker.END.toString().getBytes())});

    public static final ProducerRecord<String, JsonNode> START_RECORD = new ProducerRecord<String, JsonNode>("resource", 0, null, null, START_HEADERS);
    public static final ProducerRecord<String, JsonNode> END_RECORD = new ProducerRecord<String, JsonNode>("resource", 0, null, null, END_HEADERS);

    // Create a stream of resources, uniquely identifiable by their type, which is their position in the stream (first resource has type "0", second has type "1", etc.)
    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("resource", 0, null, REQUEST_FACTORY_NODE.apply(i), REQUEST_HEADERS));

    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> NO_RESOURCE_RULES_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("resource", 0, null, NO_RESOURCE_RULES_REQUEST_FACTORY_NODE.apply(i), REQUEST_HEADERS));

    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> REDACTED_RESOURCE_RULES_RECORD_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("resource", 0, null, NO_RESOURCE_RULES_REQUEST_FACTORY_NODE.apply(i), REQUEST_HEADERS));

    public static final Supplier<Stream<ProducerRecord<String, JsonNode>>> NO_SUCH_POLICY_NODE_FACTORY = () -> Stream.iterate(0, i -> i + 1)
            .map(i -> new ProducerRecord<String, JsonNode>("resource", 0, null, NO_SUCH_POLICY_NODE_FACTORY_NODE.apply(i), REQUEST_HEADERS));
}
