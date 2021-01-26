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
package uk.gov.gchq.palisade.service.data.stream.config;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.stream.javadsl.MergeHub;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.gchq.palisade.service.data.model.AuditErrorMessage;
import uk.gov.gchq.palisade.service.data.model.AuditSuccessMessage;
import uk.gov.gchq.palisade.service.data.model.TokenMessagePair;
import uk.gov.gchq.palisade.service.data.stream.SerDesConfig;
import uk.gov.gchq.palisade.service.data.stream.StreamComponents;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

/**
 * Configuration for all kafka connections for the data-service
 */
@Configuration
public class AkkaComponentsConfig {

    private static final StreamComponents<String, AuditErrorMessage> ERROR_COMPONENTS = new StreamComponents<>();
    private static final StreamComponents<String, AuditSuccessMessage> SUCCESS_COMPONENTS = new StreamComponents<>();

    @Bean
    AdminClient adminClient(final ActorSystem actorSystem) {
        final List<? extends Config> servers = actorSystem.settings().config().getConfigList("akka.discovery.config.services.kafka.endpoints");
        final String bootstrap = servers.stream().map(config -> String.format("%s:%d", config.getString("host"), config.getInt("port"))).collect(Collectors.joining(","));

        return AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, bootstrap));
    }

    @Bean
    Source<TokenMessagePair, Sink<TokenMessagePair, NotUsed>> auditSourceAndSink() {
      return   MergeHub.of(TokenMessagePair.class);
    }

    @Bean
    Sink<ProducerRecord<String, AuditErrorMessage>, CompletionStage<Done>> plainErrorSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AuditErrorMessage> producerSettings = ERROR_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.errorKeySerializer(),
                SerDesConfig.errorValueSerializer());

        return ERROR_COMPONENTS.plainProducer(producerSettings);
    }

    @Bean
    Sink<ProducerRecord<String, AuditSuccessMessage>, CompletionStage<Done>> plainSuccessSink(final ActorSystem actorSystem) {
        ProducerSettings<String, AuditSuccessMessage> producerSettings = SUCCESS_COMPONENTS.producerSettings(
                actorSystem,
                SerDesConfig.successKeySerializer(),
                SerDesConfig.successValueSerializer());

        return SUCCESS_COMPONENTS.plainProducer(producerSettings);
    }
}

