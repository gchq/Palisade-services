package uk.gov.gchq.palisade.service.palisade;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class PalisadeApplication {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(PalisadeApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

}
