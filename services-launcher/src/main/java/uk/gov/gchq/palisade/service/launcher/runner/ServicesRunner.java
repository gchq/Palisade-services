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

import org.apache.tools.ant.util.JavaEnvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import uk.gov.gchq.palisade.service.launcher.config.DefaultsConfiguration;
import uk.gov.gchq.palisade.service.launcher.config.OverridableConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServicesRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRunner.class);

    private List<OverridableConfiguration> configsFromFile;
    private DefaultsConfiguration defaultsConfiguration;

    public ServicesRunner(final List<OverridableConfiguration> serviceConfigurations, final DefaultsConfiguration defaultsConfiguration) {
        this.configsFromFile = serviceConfigurations;
        this.defaultsConfiguration = defaultsConfiguration;
    }

    File getServicesRoot() {
        File parent = new File(".").getAbsoluteFile();
        while (parent != null && !defaultsConfiguration.getRoot().equals(parent.getName())) {
            parent = parent.getParentFile();
        }
        return parent;
    }

    List<ProcessBuilder> constructProcessRunners(final List<OverridableConfiguration> configs) {
        return configs.stream()
                .map((config) -> {
                    String[] command = createCommand(config);
                    return new ProcessBuilder()
                            .command(command)
                            .directory(getServicesRoot())
                            .redirectOutput(new File(config.getLog()))
                            .redirectError(new File(config.getLog()));
                })
                .collect(Collectors.toList());
    }

    List<OverridableConfiguration> loadConfigurations(final List<OverridableConfiguration> configurations, final ApplicationArguments args) {
        List<OverridableConfiguration> addedConfigs = new ArrayList<>();
        Set<OverridableConfiguration> removedConfigs = new HashSet<>();

        if (args.getSourceArgs().length > 0) {
            // --enable=<service-name>
            for (String serviceName : args.getOptionValues("enable")) {
                OverridableConfiguration config = new OverridableConfiguration();
                config.setName(serviceName);
                addedConfigs.add(config);
            }
            // --disable=<service-name>
            for (String serviceName : args.getOptionValues("disable")) {
                OverridableConfiguration config = new OverridableConfiguration();
                config.setName(serviceName);
                removedConfigs.add(config);
            }
        }

        return Stream.of(configurations, addedConfigs)
                .flatMap(Collection::stream)
                .filter((x) -> !removedConfigs.contains(x))
                .map((x) -> x.defaults(defaultsConfiguration))
                .collect(Collectors.toList());
    }

    List<Process> launchApplicationsFromProcessBuilders(final List<ProcessBuilder> configurations) {
        return configurations.stream()
                .map((pb) -> {
                    try {
                        Process process = pb.start();
                        return process;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    Map<Process, Integer> joinProcesses(final List<Process> processes) {
        return processes.stream()
                .map((p) -> {
                    try {
                        return new SimpleEntry<>(p, p.waitFor());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    String[] createCommand(final OverridableConfiguration config) {

        if (config.getName().equals("policy-service")/* || config.getName().equals("data-service")*/) {

            return new String[]{
                    JavaEnvUtils.getJreExecutable("java"),
                    "-cp", String.format(config.getClasspath()),
                    String.format("-Dspring.config.location=%s", config.getConfig()),
                    String.format("-Dspring.profiles.active=%s", config.getProfiles()),
                    String.format("-Dloader.main=%s", config.getMain()),
                    config.getLoader()
            };
        } else {

            return new String[]{
                    JavaEnvUtils.getJreExecutable("java"),
                    "-jar",
                    String.format("-Dspring.config.location=%s", config.getConfig()),
                    String.format("-Dspring.profiles.active=%s", config.getProfiles()),
                    config.getTarget()
            };
        }
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        LOGGER.info(String.format("Loaded %s service configurations from file", configsFromFile.size()));

        // Build a set of configurations for processes from config and commandline args
        List<OverridableConfiguration> serviceConfigurations = loadConfigurations(configsFromFile, args);
        LOGGER.info(String.format("Loaded %s configurations total", serviceConfigurations.size()));

        List<ProcessBuilder> processBuilders = constructProcessRunners(serviceConfigurations);
        for (ProcessBuilder processBuilder : processBuilders) {
            LOGGER.info(processBuilder.command().toString());
        }
        LOGGER.info(String.format("Prepared %s processes to run", processBuilders.size()));

        // Start processes for each service configuration
        List<Process> processes = launchApplicationsFromProcessBuilders(processBuilders);
        LOGGER.info(String.format("Launched %s processes, waiting to join", processes.size()));

        // Wait for retcodes
        Map<Process, Integer> retCodes = joinProcesses(processes);
        LOGGER.info(String.format("Joined with retcodes: %s", retCodes.toString()));
    }
}
