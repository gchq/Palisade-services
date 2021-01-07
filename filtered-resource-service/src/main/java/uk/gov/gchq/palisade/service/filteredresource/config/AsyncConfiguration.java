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

package uk.gov.gchq.palisade.service.filteredresource.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Spring bean configuration and dependency injection graph for setting up async {@link java.util.concurrent.Executor}s
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties({AsyncConfigProperties.class})
public class AsyncConfiguration implements AsyncConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncConfiguration.class);

    private final AsyncConfigProperties asyncConfigProperties;

    /**
     * Autowired constructor for injecting dependant bean.
     * This can't be done through {@link Bean} method arguments as we are implementing the AsyncConfigurer interface.
     *
     * @param asyncConfigProperties spring config for async core pool size
     */
    public AsyncConfiguration(final AsyncConfigProperties asyncConfigProperties) {
        this.asyncConfigProperties = asyncConfigProperties;
    }

    @Override
    @Bean("applicationTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("AppThreadPool-");
        ex.setCorePoolSize(asyncConfigProperties.getCorePoolSize());
        LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
        return ex;
    }

}
