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
package uk.gov.gchq.palisade.service.data.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.reader.HadoopDataReader;
import uk.gov.gchq.palisade.reader.common.DataReader;
import uk.gov.gchq.palisade.reader.common.SerialisedDataReader;
import uk.gov.gchq.palisade.service.data.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.data.service.AuditService;
import uk.gov.gchq.palisade.service.data.service.DataService;
import uk.gov.gchq.palisade.service.data.service.PalisadeService;
import uk.gov.gchq.palisade.service.data.service.SimpleDataService;
import uk.gov.gchq.palisade.service.data.web.AuditClient;
import uk.gov.gchq.palisade.service.data.web.PalisadeClient;

import java.io.IOException;
import java.util.Objects;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * Simple data service bean created with instances of auditService, palisadeService and dataReader
     * which is used by a simple implementation of {@link DataService} to connect to different data storage technologies and deserialise the data
     *
     * @param auditService    the audit service
     * @param palisadeService the palisade service
     * @param dataReader      the data reader
     * @return the simple data service
     */
    @Bean
    public SimpleDataService simpleDataService(final AuditService auditService,
                                               final PalisadeService palisadeService,
                                               final DataReader dataReader) {
        return new SimpleDataService(auditService, palisadeService, dataReader);
    }

    /**
     * Bean implementation for {@link HadoopDataReader} which extends {@link SerialisedDataReader} and is used for setting hadoopConfigurations and reading raw data.
     *
     * @return a new instance of {@link HadoopDataReader}
     * @throws IOException ioException
     */
    @Bean
    public DataReader hadoopDataReader() throws IOException {
        return new HadoopDataReader();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

}
