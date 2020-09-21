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

package uk.gov.gchq.palisade.service.attributemask.stream.config;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import uk.gov.gchq.palisade.service.attributemask.stream.PropertiesConfigurer;

/**
 * Convert Spring YAML configuration to Akka HOCON configuration and provide core Akka beans
 */
@Configuration
@ConditionalOnProperty(
        value = "akka.discovery.config.services.kafka.from-config",
        havingValue = "true",
        matchIfMissing = true
)
public class AkkaSystemConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaSystemConfig.class);

    @Bean
    @ConditionalOnMissingBean
    PropertiesConfigurer propertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
        return new PropertiesConfigurer(resourceLoader, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    ActorSystem getActorSystem(final PropertiesConfigurer propertiesConfigurer) {
        propertiesConfigurer.getAllActiveProperties()
                .forEach((key, value) -> LOGGER.info("{} = {}", key, value));
        return ActorSystem.create("SpringAkkaStreamsSystem", propertiesConfigurer.toHoconConfig(propertiesConfigurer.getAllActiveProperties()));
    }

    @Bean
    @ConditionalOnMissingBean
    Materializer getMaterializer(final ActorSystem system) {
        return Materializer.createMaterializer(system);
    }

}
