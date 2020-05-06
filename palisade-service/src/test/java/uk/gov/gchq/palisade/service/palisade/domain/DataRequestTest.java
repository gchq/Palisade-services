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

package uk.gov.gchq.palisade.service.palisade.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.palisade.repository.DataRequestRepository;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD) //reset db after each test
@ActiveProfiles("dbtest")
public class DataRequestTest {

    @Autowired
    private DataRequestRepository dataRequestRepository;

    @Test
    public void storeAndRetrieveTest() {
        final Context context = new Context(Stream.of(new SimpleEntry<String, Object>("testing repo", "this")).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));
        final DataRequestConfig dataRequestConfig = new DataRequestConfig();
        dataRequestConfig.setContext(context);
        dataRequestConfig.setUser(new User().userId("archibald"));
        dataRequestConfig.setOriginalRequestId(new RequestId().id("identifier-x"));
        dataRequestConfig.setRules(Collections.emptyMap());

        final DataRequestEntity entity = new DataRequestEntity(dataRequestConfig);

        this.dataRequestRepository.save(entity);
        final DataRequestEntity subject = this.dataRequestRepository.getByRequestId("identifier-x");

        assertThat("The user id is not preserved through persistence", subject.getUser().getUserId().getId(), is(equalTo("archibald")));
        assertThat("The request id is not preserved through persistence", subject.getRequestId(), is(equalTo("identifier-x")));
        assertThat("The context is not preserved through persistence", subject.getContext().get("testing repo"), is(equalTo("this")));
    }

}
