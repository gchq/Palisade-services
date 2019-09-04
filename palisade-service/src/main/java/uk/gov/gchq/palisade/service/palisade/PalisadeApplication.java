package uk.gov.gchq.palisade.service.palisade;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.gchq.palisade.service.palisade.service.PalisadeService;

@SpringBootApplication
//@EnableFeignClients
public class PalisadeApplication {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(PalisadeApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

    @Configuration
    static class Config {

        @Bean
        public PalisadeService palisadeService() {
            return null;
        }

    }

}
