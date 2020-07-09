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
package uk.gov.gchq.palisade.service.queryscope;


import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Temporary set-up of a Spring Boot Application class.  Being used in this basic form for the configuration
 * of the prototype classes QueryScopeRequest and QueryScopeResponse and the related tests.
 */
@SpringBootApplication
public class QueryScopeApplication {


    /**
     * SpringBoot application entry-point method for the {@link QueryScopeApplication} executable
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(final String[] args) {
        new SpringApplicationBuilder(QueryScopeApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }
}
