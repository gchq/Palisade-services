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
        ActorSystem system = ActorSystem.create("SpringAkkaStreamsSystem", propertiesConfigurer.toHoconConfig(propertiesConfigurer.getAllActiveProperties()));
        system.log(); // TODO: delete me
        return system;
    }

    @Bean
    @ConditionalOnMissingBean
    Materializer getMaterializer(final ActorSystem system) {
        return Materializer.createMaterializer(system);
    }

}
