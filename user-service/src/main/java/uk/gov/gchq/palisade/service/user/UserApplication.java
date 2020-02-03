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

package uk.gov.gchq.palisade.service.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.service.user.service.SimpleUserService;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@EnableEurekaClient
@EnableFeignClients
@EnableCaching
@SpringBootApplication
public class UserApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserApplication.class);

    @Autowired
    SimpleUserService simpleUserService;

    public static void main(final String[] args) {
        LOGGER.debug("UserApplication started with: {}", UserApplication.class.toString(), "main", Arrays.toString(args));
        new SpringApplicationBuilder(UserApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    @PostConstruct
    public void loadUsers() throws IOException {
        Resource resource = new ClassPathResource("users.txt");
        InputStream inputStream = resource.getInputStream();
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(inputStream);
            String data = new String(bdata, StandardCharsets.UTF_8);
            User newUser = new User().userId(data);
            simpleUserService.addUserToCache(newUser);
            LOGGER.info("Users {} added to cache", newUser);
        } catch (IOException e) {
            LOGGER.error("IOException", e);
        }
    }
}
