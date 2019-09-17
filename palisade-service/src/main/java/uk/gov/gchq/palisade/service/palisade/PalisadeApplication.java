package uk.gov.gchq.palisade.service.palisade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.gov.gchq.palisade.service.palisade.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.palisade.repository.BackingStore;
import uk.gov.gchq.palisade.service.palisade.repository.HashMapBackingStore;
import uk.gov.gchq.palisade.service.palisade.repository.K8sBackingStore;
import uk.gov.gchq.palisade.service.palisade.repository.PropertiesBackingStore;
import uk.gov.gchq.palisade.service.palisade.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.palisade.service.AuditService;
import uk.gov.gchq.palisade.service.palisade.service.CacheService;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;
import uk.gov.gchq.palisade.service.palisade.service.PolicyService;
import uk.gov.gchq.palisade.service.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.palisade.service.SimplePalisadeService;
import uk.gov.gchq.palisade.service.palisade.service.UserService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

@SpringBootApplication
@EnableFeignClients
public class PalisadeApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(PalisadeApplication.class);

    public static void main(final String[] args) {
        new SpringApplicationBuilder(PalisadeApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    @Configuration
    static class Config implements AsyncConfigurer {

        @Autowired
        public Map<String, BackingStore> backingStores;

        @Value("${cache.implementation.props:cache.props}")
        public String propertyFile;

        @Bean
        public PalisadeService palisadeService() {
            return new SimplePalisadeService(auditService(), userService(), policyService(), resourceService(), cacheService(), getAsyncExecutor());
        }

        @Bean
        public UserService userService() {
            return new UserService(auditService(), getAsyncExecutor());
        }

        @Bean
        public AuditService auditService() {
            return new AuditService(getAsyncExecutor());
        }

        @Bean
        public ResourceService resourceService() {
            return new ResourceService(getAsyncExecutor());
        }

        @Bean
        public PolicyService policyService() {
            return new PolicyService(getAsyncExecutor());
        }

        @Bean(name = "hashmap")
        @ConditionalOnProperty(prefix = "cache", name = "implementation", havingValue = "hashmap", matchIfMissing = true)
        public HashMapBackingStore hashMapBackingStore() {
            return new HashMapBackingStore();
        }

        @Bean(name = "k8s")
        @ConditionalOnProperty(prefix = "cache", name = "implementation", havingValue = "k8s")
        public K8sBackingStore k8sBackingStore() {
            return new K8sBackingStore();
        }

        @Bean(name = "props")
        @ConditionalOnProperty(prefix = "cache", name = "implementation", havingValue = "props")
        public PropertiesBackingStore propertiesBackingStore() {
            return new PropertiesBackingStore(Optional.ofNullable(this.propertyFile).orElse("cache.properties"));
        }

        @Bean
        public CacheService cacheService() {
            return Optional.of(new SimpleCacheService()).stream().peek(cache -> {
                LOGGER.info("Cache backing implementation = {}", this.backingStores.values().stream().findFirst().orElse(null).getClass().getSimpleName());
                cache.backingStore(this.backingStores.values().stream().findFirst().orElse(null));
                //return cache;
            }).findFirst().orElse(null);
        }

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }

        @Override
        @Bean("threadPoolTaskExecutor")
        public Executor getAsyncExecutor() {
            return Optional.of(new ThreadPoolTaskExecutor()).stream().peek(ex -> {
                ex.setThreadNamePrefix("AppThreadPool-");
                ex.setCorePoolSize(5);
                LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
            }).findFirst().orElse(null);
        }

        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return new ApplicationAsyncExceptionHandler();
        }

    }

}
