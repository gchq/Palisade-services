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
package uk.gov.gchq.palisade.service.data.request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.data.service.SimpleDataServiceTest;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ReadRequestTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataServiceTest.class);
    ReadRequest readRequest = new ReadRequest();

    @Before
    public void setup() {
        LOGGER.info("Simple Data Service created: {}", readRequest);
    }


    @Test
    public void getToken() {
        //given
        String token = "token";
        readRequest.token(token);

        //then
        String readToken = readRequest.getToken();

        //when
        assertEquals(readToken, token);

    }

    @Test
    public void getResource() {
        //given
        LeafResource resource = Mockito.mock(LeafResource.class);
        readRequest.resource(resource);

        //when
        LeafResource readResource = readRequest.getResource();

        //then
        assertEquals(readResource, resource);
    }
}