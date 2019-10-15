package uk.gov.gchq.palisade.service.audit.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.service.audit.AuditService;
import uk.gov.gchq.palisade.service.audit.service.LoggerAuditService;
import uk.gov.gchq.palisade.service.audit.service.StroomAuditService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ApplicationConfiguration.class)
@ActiveProfiles(profiles = "")
public class ApplicationConfigurationTest {

    private static final Set<Class> expectedAudits = new HashSet<>();

    static {
        expectedAudits.add(LoggerAuditService.class);
        expectedAudits.add(StroomAuditService.class);
    }

    @Autowired
    public Map<String, AuditService> auditServices;

    @Test
    public void auditServicesLoaded() {
        assertThat(auditServices, not(equalTo(nullValue())));
    }

    @Test
    public void configurationDefinesLoadedServices() {
        // Given - expectedAudits
        // Then
        for (AuditService auditService : auditServices.values()) {
            assertThat(auditService.getClass(), isIn(expectedAudits));
        }
    }
}
