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

package uk.gov.gchq.palisade.service.manager;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * SpringBoot application entry-point class
 */
@EnableEurekaClient
@EnableFeignClients
@SpringBootApplication
public class ManagerApplication {

    public ManagerApplication() {
        // no-args constructor needed for serialization only
    }

    /**
     * Application entry-point method
     * Will later call out to the configured runner to run for the rest of the lifetime of the application
     * @param args
     */
    public static void main(final String[] args) {
        new SpringApplicationBuilder(ManagerApplication.class).run(args);
    }
}
