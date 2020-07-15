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
package uk.gov.gchq.palisade.service.data.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class AuditErrorMessageTest {

    @Autowired
    private JacksonTester<AuditErrorMessage> jsonTester;

    /**
     * Create the object with the builder and then convert to the Json equivalent.
     *
     * @throws IOException throws if the UserResponse object cannot be converted to a JsonContent.
     *                     This equates to a failure to serialise the string.
     */
    @Test
    public void testSerialiseAuditErrorMessageToJson() throws IOException {

        Context context = new Context().purpose("testContext");

        String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        AuditErrorMessage auditErrorMessage = AuditErrorMessage.Builder.create()
                .withUserId("originalUserID")
                .withResourceId(
                        "testResourceId")
                .withContext(context)
                .withError(new InternalError("Something went wrong!"));

        JsonContent<AuditErrorMessage> auditErrorMessageJsonContent = jsonTester.write(auditErrorMessage);

        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("originalUserID");
        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");
        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.serviceName").isEqualTo("data-service");
        assertThat(auditErrorMessageJsonContent).extractingJsonPathStringValue("$.error.message").isEqualTo("Something went wrong!");

    }

    /**
     * Create the object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testDeserialiseJsonToAuditErrorMessage() throws IOException {


        String jsonString = "{\"userId\":\"originalUserID\",\"resourceId\":\"testResourceId\",\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"error\":{\"cause\":null,\"stackTrace\":[{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"testSerialiseAuditErrorMessageToJson\",\"fileName\":\"AuditErrorMessageTest.java\",\"lineNumber\":64,\"className\":\"uk.gov.gchq.palisade.service.data.request.AuditErrorMessageTest\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"invoke0\",\"fileName\":\"NativeMethodAccessorImpl.java\",\"lineNumber\":-2,\"className\":\"jdk.internal.reflect.NativeMethodAccessorImpl\",\"nativeMethod\":true},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"invoke\",\"fileName\":\"NativeMethodAccessorImpl.java\",\"lineNumber\":62,\"className\":\"jdk.internal.reflect.NativeMethodAccessorImpl\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"invoke\",\"fileName\":\"DelegatingMethodAccessorImpl.java\",\"lineNumber\":43,\"className\":\"jdk.internal.reflect.DelegatingMethodAccessorImpl\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"invoke\",\"fileName\":\"Method.java\",\"lineNumber\":566,\"className\":\"java.lang.reflect.Method\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"runReflectiveCall\",\"fileName\":\"FrameworkMethod.java\",\"lineNumber\":50,\"className\":\"org.junit.runners.model.FrameworkMethod$1\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"run\",\"fileName\":\"ReflectiveCallable.java\",\"lineNumber\":12,\"className\":\"org.junit.internal.runners.model.ReflectiveCallable\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"invokeExplosively\",\"fileName\":\"FrameworkMethod.java\",\"lineNumber\":47,\"className\":\"org.junit.runners.model.FrameworkMethod\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"InvokeMethod.java\",\"lineNumber\":17,\"className\":\"org.junit.internal.runners.statements.InvokeMethod\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"RunBeforeTestExecutionCallbacks.java\",\"lineNumber\":74,\"className\":\"org.springframework.test.context.junit4.statements.RunBeforeTestExecutionCallbacks\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"RunAfterTestExecutionCallbacks.java\",\"lineNumber\":84,\"className\":\"org.springframework.test.context.junit4.statements.RunAfterTestExecutionCallbacks\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"RunBeforeTestMethodCallbacks.java\",\"lineNumber\":75,\"className\":\"org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"RunAfterTestMethodCallbacks.java\",\"lineNumber\":86,\"className\":\"org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"SpringRepeat.java\",\"lineNumber\":84,\"className\":\"org.springframework.test.context.junit4.statements.SpringRepeat\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"runLeaf\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":325,\"className\":\"org.junit.runners.ParentRunner\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"runChild\",\"fileName\":\"SpringJUnit4ClassRunner.java\",\"lineNumber\":251,\"className\":\"org.springframework.test.context.junit4.SpringJUnit4ClassRunner\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"runChild\",\"fileName\":\"SpringJUnit4ClassRunner.java\",\"lineNumber\":97,\"className\":\"org.springframework.test.context.junit4.SpringJUnit4ClassRunner\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"run\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":290,\"className\":\"org.junit.runners.ParentRunner$3\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"schedule\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":71,\"className\":\"org.junit.runners.ParentRunner$1\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"runChildren\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":288,\"className\":\"org.junit.runners.ParentRunner\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"access$000\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":58,\"className\":\"org.junit.runners.ParentRunner\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":268,\"className\":\"org.junit.runners.ParentRunner$2\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"RunBeforeTestClassCallbacks.java\",\"lineNumber\":61,\"className\":\"org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"evaluate\",\"fileName\":\"RunAfterTestClassCallbacks.java\",\"lineNumber\":70,\"className\":\"org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"run\",\"fileName\":\"ParentRunner.java\",\"lineNumber\":363,\"className\":\"org.junit.runners.ParentRunner\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"run\",\"fileName\":\"SpringJUnit4ClassRunner.java\",\"lineNumber\":190,\"className\":\"org.springframework.test.context.junit4.SpringJUnit4ClassRunner\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"run\",\"fileName\":\"JUnitCore.java\",\"lineNumber\":137,\"className\":\"org.junit.runner.JUnitCore\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"startRunnerWithArgs\",\"fileName\":\"JUnit4IdeaTestRunner.java\",\"lineNumber\":68,\"className\":\"com.intellij.junit4.JUnit4IdeaTestRunner\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"startRunnerWithArgs\",\"fileName\":\"IdeaTestRunner.java\",\"lineNumber\":33,\"className\":\"com.intellij.rt.junit.IdeaTestRunner$Repeater\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"prepareStreamsAndStart\",\"fileName\":\"JUnitStarter.java\",\"lineNumber\":230,\"className\":\"com.intellij.rt.junit.JUnitStarter\",\"nativeMethod\":false},{\"classLoaderName\":null,\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"main\",\"fileName\":\"JUnitStarter.java\",\"lineNumber\":58,\"className\":\"com.intellij.rt.junit.JUnitStarter\",\"nativeMethod\":false}],\"message\":\"Something went wrong!\",\"suppressed\":[],\"localizedMessage\":\"Something went wrong!\"},\"serveHostName\":\"CIC00948.lan\",\"serviceName\":\"data-service\",\"timestamp\":\"2020-07-15T08:31:03.411608Z\",\"serverIP\":\"192.168.2.226\",\"serverHostname\":\"CIC00948.lan\",\"attributes\":{}}";

        ObjectContent<AuditErrorMessage> auditSuccessMessageObjectContent = jsonTester.parse(jsonString);

        AuditErrorMessage auditErrorMessage = auditSuccessMessageObjectContent.getObject();
        assertThat(auditErrorMessage.getUserId()).isEqualTo("originalUserID");
        assertThat(auditErrorMessage.getResourceId()).isEqualTo("testResourceId");
        assertThat(auditErrorMessage.getContext().getPurpose()).isEqualTo("testContext");
        assertThat(auditErrorMessage.getError().getMessage()).isEqualTo("Something went wrong!");

    }

}