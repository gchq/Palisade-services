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

package uk.gov.gchq.palisade.service.launcher.runner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import uk.gov.gchq.palisade.service.launcher.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.launcher.config.DefaultsConfiguration;
import uk.gov.gchq.palisade.service.launcher.config.OverridableConfiguration;
import uk.gov.gchq.palisade.service.launcher.config.ServicesConfiguration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ApplicationConfiguration.class)
public class ServicesRunnerTest {

    @Autowired
    private ServicesConfiguration servicesConfiguration;
    @Autowired
    private DefaultsConfiguration defaultsConfiguration;

    @Mock
    private ProcessBuilder processBuilder;
    @Mock
    private Process process;
    @Mock
    private ApplicationArguments arguments;

    private ServicesRunner servicesRunner;
    private List<OverridableConfiguration> serviceConfigurations;

    @Before
    public void setUp() {
        serviceConfigurations = servicesConfiguration.getServices();
        servicesRunner = new ServicesRunner(serviceConfigurations, defaultsConfiguration);
    }

    private OverridableConfiguration getTestConfig() {
        OverridableConfiguration expected = new OverridableConfiguration();
        expected.setName("dummy-service");
        expected.setTarget("target");
        expected.setConfig(expected.getName() + "-config");
        expected.setLog("log");

        return expected;
    }

    @Test
    public void configurationsLoadedFromTestConfig() {
        // Given
        OverridableConfiguration expected = getTestConfig();

        // When - Autowired serviceConfigurations and defaultsConfiguration

        // Then
        assertThat(serviceConfigurations.size(), equalTo(1));
        assertThat(serviceConfigurations.get(0).defaults(defaultsConfiguration), equalTo(expected));
    }

    @Test
    public void commandLineExtendsConfigurations() {
        // Given - ./services-launcher.jar --enable=mock-service
        Set<OverridableConfiguration> configs = new HashSet<>();
        configs.add(getTestConfig());
        when(arguments.getOptionValues(eq("enable"))).thenReturn(Collections.singletonList("mock-service"));
        when(arguments.getSourceArgs()).thenReturn(new String[] {"--enable=mock-service"});

        // When
        Set<OverridableConfiguration> commandLineExtensions = servicesRunner.loadConfigurations(configs, arguments)
                .collect(Collectors.toSet());

        // Then
        Set<OverridableConfiguration> expected = servicesRunner.loadConfigurations(configs)
                .collect(Collectors.toSet());
        OverridableConfiguration expectedExtension = new OverridableConfiguration().defaults(defaultsConfiguration);
        expectedExtension.setName("mock-service");
        expected.add(expectedExtension);
        assertThat(commandLineExtensions, equalTo(expected));
    }

    @Test
    public void processBuilderConstructedFromConfiguration() {
        // Given
        OverridableConfiguration config = getTestConfig();

        // When
        ProcessBuilder processBuilder = servicesRunner.constructServiceProcess(config);

        // Then
        assertThat(processBuilder.command(), hasItem(config.getTarget()));
        assertThat(processBuilder.directory(), equalTo(servicesRunner.getServicesRoot()));
    }

    @Test
    public void processesLaunchedFromBuilders() throws IOException {
        // Given
        ProcessBuilder[] processBuilders = new ProcessBuilder[] {processBuilder};
        when(processBuilder.start()).thenReturn(process);

        // When
        Stream<Process> processes = servicesRunner.launchApplicationsFromProcessBuilders(Arrays.stream(processBuilders));

        // Then
        assertThat(processes.collect(Collectors.toList()), hasItem(process));
    }

    @Test
    public void processesJoined() throws InterruptedException {
        // Given
        Integer retCode = 0;
        Process[] processes = new Process[] {process};
        when(process.waitFor()).thenReturn(retCode);

        // When
        Map<Process, Integer> retCodes = servicesRunner.joinProcesses(Arrays.stream(processes));

        // Then
        assertThat(retCodes.get(process), equalTo(retCode));
    }
}
