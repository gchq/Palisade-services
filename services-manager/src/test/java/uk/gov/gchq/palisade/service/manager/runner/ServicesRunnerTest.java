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

package uk.gov.gchq.palisade.service.manager.runner;

import org.junit.Ignore;

@Ignore
public class ServicesRunnerTest {

    //@Autowired
    //private ServicesConfiguration servicesConfiguration;
    //@Autowired
    //private DefaultsConfiguration defaultsConfiguration;

    //@Mock
    //private ProcessBuilder processBuilder;
    //@Mock
    //private Process process;
    //@Mock
    //private ApplicationArguments arguments;

    //private ServicesRunner servicesRunner;
    //private List<OverridableConfiguration> serviceConfigurations;

    //private static <T> ArrayList<T> singleton(T element) {
    //    ArrayList<T> singletonList = new ArrayList<>();
    //    singletonList.add(element);
    //    return singletonList;
    //}

    //@Before
    //public void setUp() {
    //    serviceConfigurations = servicesConfiguration.getServices();
    //    servicesRunner = new ServicesRunner(serviceConfigurations, defaultsConfiguration);
    //}

    //private OverridableConfiguration getTestConfig() {
    //    OverridableConfiguration expected = new OverridableConfiguration();
    //    expected.setName("dummy-service");
    //    expected.setTarget("target");
    //    expected.setConfig(expected.getName() + "-config");
    //    expected.setLog("log");

    //    return expected;
    //}

    //@Test
    //public void configurationsLoadedFromTestConfig() {
    //    // Given
    //    OverridableConfiguration expected = getTestConfig();

    //    // When - Autowired serviceConfigurations and defaultsConfiguration

    //    // Then
    //    assertThat(serviceConfigurations.size(), equalTo(1));
    //    assertThat(serviceConfigurations.get(0).defaults(defaultsConfiguration), equalTo(expected));
    //}

    //@Test
    //public void commandLineExtendsConfigurations() {
    //    // Given - ./services-launcher.jar --enable=another-mock-service
    //    List<OverridableConfiguration> configs = singleton(getTestConfig());
    //    when(arguments.getOptionValues(eq("enable"))).thenReturn(singleton("another-mock-service"));
    //    when(arguments.getSourceArgs()).thenReturn(new String[] {"--enable=another-mock-service"});

    //    // When
    //    List<OverridableConfiguration> commandLineExtensions = servicesRunner.loadConfigurations(configs, arguments);

    //    // Then
    //    List<OverridableConfiguration> expected = configs.stream().map(x -> x.defaults(defaultsConfiguration)).collect(Collectors.toList());
    //    OverridableConfiguration expectedExtension = new OverridableConfiguration().defaults(defaultsConfiguration);
    //    expectedExtension.setName("another-mock-service");
    //    expected.add(expectedExtension);
    //    assertThat(commandLineExtensions, equalTo(expected));
    //}

    //@Test
    //public void processBuilderConstructedFromConfiguration() {
    //    // Given
    //    OverridableConfiguration config = getTestConfig();

    //    // When
    //    ProcessBuilder processBuilder = servicesRunner.constructProcessRunners(singleton(config)).get(0);

    //    // Then
    //    assertThat(processBuilder.command(), hasItem(config.getTarget()));
    //    assertThat(processBuilder.directory(), equalTo(servicesRunner.getServicesRoot()));
    //}

    //@Test
    //public void processesLaunchedFromBuilders() throws IOException {
    //    // Given
    //    List<ProcessBuilder> processBuilders = singleton(processBuilder);
    //    when(processBuilder.start()).thenReturn(process);

    //    // When
    //    List<Process> processes = servicesRunner.launchApplicationsFromProcessBuilders(processBuilders);

    //    // Then
    //    assertThat(processes, hasItem(process));
    //}

    //@Test
    //public void processesJoined() throws InterruptedException {
    //    // Given
    //    Integer retCode = 0;
    //    List<Process> processes = singleton(process);
    //    when(process.waitFor()).thenReturn(retCode);

    //    // When
    //    Map<Process, Integer> retCodes = servicesRunner.joinProcesses(processes);

    //    // Then
    //    assertThat(retCodes.get(process), equalTo(retCode));
    //}
}
