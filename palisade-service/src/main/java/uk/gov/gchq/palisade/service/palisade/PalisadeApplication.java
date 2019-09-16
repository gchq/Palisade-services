package uk.gov.gchq.palisade.service.palisade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.gov.gchq.palisade.service.palisade.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.palisade.repository.SimpleCacheService;
import uk.gov.gchq.palisade.service.palisade.service.AuditService;
import uk.gov.gchq.palisade.service.palisade.service.CacheService;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;
import uk.gov.gchq.palisade.service.palisade.service.PolicyService;
import uk.gov.gchq.palisade.service.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.palisade.service.SimplePalisadeService;
import uk.gov.gchq.palisade.service.palisade.service.UserService;

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

        @Bean
        public CacheService cacheService() {
            return new SimpleCacheService();
        }

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }

        @Override
        @Bean("threadPoolTaskExecutor")
        public Executor getAsyncExecutor() {
            ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
            ex.setThreadNamePrefix("AppThreadPool-");
            LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
            return ex;
        }

        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return new ApplicationAsyncExceptionHandler();
        }

    }

}
