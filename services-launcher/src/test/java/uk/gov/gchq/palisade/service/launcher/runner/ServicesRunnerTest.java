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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import uk.gov.gchq.palisade.service.launcher.config.ApplicationConfiguration;
import uk.gov.gchq.palisade.service.launcher.config.DefaultsConfiguration;
import uk.gov.gchq.palisade.service.launcher.config.OverridableConfiguration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static uk.gov.gchq.palisade.service.launcher.runner.ServicesRunner.constructServiceProcess;
import static uk.gov.gchq.palisade.service.launcher.runner.ServicesRunner.getServicesRoot;
import static uk.gov.gchq.palisade.service.launcher.runner.ServicesRunner.joinProcesses;
import static uk.gov.gchq.palisade.service.launcher.runner.ServicesRunner.launchApplicationsFromProcessBuilders;

@SpringBootTest(classes = ServicesRunner.class)
@RunWith(SpringRunner.class)
@Import(ApplicationConfiguration.class)
@ActiveProfiles("test")
public class ServicesRunnerTest {

    @Autowired
    private List<OverridableConfiguration> serviceConfigurations;
    @Autowired
    private DefaultsConfiguration defaultsConfiguration;

    @Mock
    private ProcessBuilder processBuilder;
    @Mock
    private Process process;

    private OverridableConfiguration getTestConfig() {
        OverridableConfiguration expected = new OverridableConfiguration();
        expected.setName("services-launcher");
        expected.setTarget("target");
        expected.setConfig(expected.getName() + "-config");
        expected.setLog("log");

        return expected;
    }

    @Test
    public void configurationsLoadedFromTestConfig() {
        // Given
        OverridableConfiguration expected = getTestConfig();

        // When - Autowired serviceConfigurations

        // Then
        assertThat(serviceConfigurations.size(), equalTo(1));
        assertThat(serviceConfigurations.get(0).defaults(defaultsConfiguration), equalTo(expected));
    }

    @Test
    public void commandLineExtendsConfigurations() {
    }

    @Test
    public void processBuilderConstructedFromConfiguration() {
        // Given
        OverridableConfiguration config = getTestConfig();

        // When
        ProcessBuilder processBuilder = constructServiceProcess(config);

        // Then
        assertThat(processBuilder.command(), contains(config.getTarget()));
        assertThat(processBuilder.directory(), equalTo(getServicesRoot()));
    }

    @Test
    public void processesLaunchedFromBuilders() throws IOException {
        // Given
        ProcessBuilder[] processBuilders = new ProcessBuilder[] {processBuilder};
        when(processBuilder.start()).thenReturn(process);

        // When
        Stream<Process> processes = launchApplicationsFromProcessBuilders(Arrays.stream(processBuilders));

        // Then
        assertThat(processes.collect(Collectors.toList()), contains(process));
    }

    @Test
    public void processesJoined() throws InterruptedException {
        // Given
        Integer retCode = 0;
        Process[] processes = new Process[] {process};
        when(process.waitFor()).thenReturn(retCode);

        // When
        Map<Process, Integer> retCodes = joinProcesses(Arrays.stream(processes));

        // Then
        assertThat(retCodes.get(process), equalTo(retCode));
    }
}
