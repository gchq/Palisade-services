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

package uk.gov.gchq.palisade.component.user.web;

import akka.Done;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.service.user.model.UserRequest;
import uk.gov.gchq.palisade.service.user.service.AsyncUserServiceProxy;
import uk.gov.gchq.palisade.service.user.service.CacheableUserServiceProxy;
import uk.gov.gchq.palisade.service.user.service.NullUserService;
import uk.gov.gchq.palisade.service.user.service.UserService;
import uk.gov.gchq.palisade.service.user.stream.ConsumerTopicConfiguration;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@Configuration
public class ApplicationTestConfiguration implements AsyncConfigurer {

    @Bean
    public AsyncUserServiceProxy asyncUserServiceProxy(@Qualifier("ignoringSink") final Sink<ProducerRecord<String, UserRequest>, CompletionStage<Done>> sink,
                                                       final ConsumerTopicConfiguration upstreamConfig,
                                                       final Materializer materializer,
                                                       final CacheableUserServiceProxy service,
                                                       final Executor executor) {
        return new AsyncUserServiceProxy(sink, upstreamConfig, materializer, service, executor);
    }

    @Bean
    public CacheableUserServiceProxy cacheableUserServiceProxy() {
        return new CacheableUserServiceProxy(new NullUserService());
    }

    @Override
    @Bean("applicationTaskExecutor")
    public Executor getAsyncExecutor() {
        return Optional.of(new ThreadPoolTaskExecutor()).stream().peek(ex -> {
            ex.setThreadNamePrefix("AppThreadPool-");
            ex.setCorePoolSize(6);
        }).findFirst().orElse(null);
    }
}
