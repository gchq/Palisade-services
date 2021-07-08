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

package uk.gov.gchq.palisade.service.data.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

/**
 * WebMVC configuration class used to create a executor bean when launching the service.
 */
@Configuration
@EnableAsync
@EnableWebMvc
@EnableConfigurationProperties({AsyncConfigProperties.class})
public class AsyncWebMvcConfiguration implements AsyncConfigurer, WebMvcConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncWebMvcConfiguration.class);

    private final AsyncConfigProperties asyncConfigProperties;

    /**
     * Default constructor used including configClass
     *
     * @param asyncConfigProperties class containing webMVC properties
     */
    public AsyncWebMvcConfiguration(final AsyncConfigProperties asyncConfigProperties) {
        this.asyncConfigProperties = asyncConfigProperties;
    }

    @Override
    @Bean("threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("AppThreadPool-");
        ex.setCorePoolSize(asyncConfigProperties.getCorePoolSize());
        LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
        return ex;
    }

    @Override
    public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(Objects.requireNonNull(getAsyncExecutor()));
        configurer.setDefaultTimeout(asyncConfigProperties.getWebMvcTimeout());
    }

}
