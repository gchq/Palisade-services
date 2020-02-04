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

package uk.gov.gchq.palisade.service.user.service;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;

import uk.gov.gchq.palisade.service.user.UserApplication;

@EnableCaching
@SpringBootTest(classes = UserApplication.class)
public class SlowMapCacheTest {
    private static Logger LOGGER = LoggerFactory.getLogger(SlowMapCacheTest.class);
    SlowMap<String, String> map = new SlowMap<>();

    @Test
    public void cachingShouldWork() {
        LOGGER.info("Put :: {}", map.put("yvon", "of the yukon"));
        LOGGER.info("Get :: {}", map.get("yvon"));
        LOGGER.info("Get :: {}", map.get("yvon"));
        LOGGER.info("Get :: {}", map.get("yvon"));
        LOGGER.info("Get :: {}", map.get("yvon"));
    }
}
