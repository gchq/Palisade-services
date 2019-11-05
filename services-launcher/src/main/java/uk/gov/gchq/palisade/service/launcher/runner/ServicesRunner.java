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
import java.util.Arrays;
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

    private List<OverridableConfiguration> serviceConfigurations;
    private DefaultsConfiguration defaultsConfiguration;

    public ServicesRunner(final List<OverridableConfiguration> serviceConfigurations, final DefaultsConfiguration defaultsConfiguration) {
        this.serviceConfigurations = serviceConfigurations;
        this.defaultsConfiguration = defaultsConfiguration;
    }

    File getServicesRoot() {
        File parent = new File(".").getAbsoluteFile();
        while (parent != null && !defaultsConfiguration.getRoot().equals(parent.getName())) {
            LOGGER.info(parent.getName(), parent.getParent());
            parent = parent.getParentFile();
        }
        return parent;
    }

    ProcessBuilder constructServiceProcess(final OverridableConfiguration config) {
        String[] command = new String[] {
                JavaEnvUtils.getJreExecutable("java"),
                "-jar", config.getTarget(),
                String.format("--spring-config-location=%s,%s", defaultsConfiguration.getConfig(), config.getConfig())
        };
        ProcessBuilder builder = new ProcessBuilder()
                .command(command)
                .directory(getServicesRoot())
                .redirectOutput(new File(config.getLog()))
                .redirectError(new File(config.getLog()));
        LOGGER.info(String.format("Constructed ProcessBuilder %s [%s]", Arrays.toString(command), builder.redirectOutput().toString()));
        return builder;
    }

    Stream<OverridableConfiguration> loadConfigurations(final Set<OverridableConfiguration> configurations, final ApplicationArguments args) {
        if (args.getSourceArgs().length > 0) {
            // --enable=<service-name>
            for (String serviceName : args.getOptionValues("enable")) {
                OverridableConfiguration config = new OverridableConfiguration();
                config.setName(serviceName);
                configurations.add(config);
            }
            // --disable=<service-name>
            for (String serviceName : args.getOptionValues("disable")) {
                OverridableConfiguration config = new OverridableConfiguration();
                config.setName(serviceName);
                configurations.remove(config);
            }
        }

        return loadConfigurations(configurations);
    }

    Stream<OverridableConfiguration> loadConfigurations(final Set<OverridableConfiguration> configurations) {
        return configurations.parallelStream()
                .map((x) -> x.defaults(defaultsConfiguration));
    }

    Stream<Process> launchApplicationsFromProcessBuilders(final Stream<ProcessBuilder> configurations) {
        Stream<Process> processes = configurations.map((pb) -> {
                    try {
                        Process process = pb.start();
                        LOGGER.info(String.format("Started process %s", process.toString()));
                        return process;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull);

        return processes;
    }

    Map<Process, Integer> joinProcesses(final Stream<Process> processes) {
        Map<Process, Integer> retCodes = processes.map((p) -> {
                    try {
                        return new SimpleEntry<>(p, p.waitFor());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        return retCodes;
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        Set<OverridableConfiguration> configurations = new HashSet<>(serviceConfigurations);

        // Build a set of configurations for processes from config and commandline args
        Stream<ProcessBuilder> processBuilders = loadConfigurations(configurations, args).map(this::constructServiceProcess);

        // Start processes for each service configuration
        Stream<Process> processes = launchApplicationsFromProcessBuilders(processBuilders);

        // Wait for retcodes
        Map<Process, Integer> retCodes = joinProcesses(processes);

        LOGGER.info(retCodes.toString());
    }
}
