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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import uk.gov.gchq.palisade.service.launcher.config.DefaultsConfiguration;
import uk.gov.gchq.palisade.service.launcher.config.OverriddenConfiguration;
import uk.gov.gchq.palisade.service.launcher.config.ServiceConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ServicesRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRunner.class);

    @Autowired
    private List<OverriddenConfiguration> serviceConfigurations;

    @Autowired
    private DefaultsConfiguration defaultsConfiguration;

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        Set<OverriddenConfiguration> configurations = new HashSet<>(serviceConfigurations);

        // --enable=<service-name>
        for (String serviceName : args.getOptionValues("enable")) {
            OverriddenConfiguration config = new OverriddenConfiguration();
            config.setName(serviceName);
            config.defaults(new DefaultsConfiguration(defaultsConfiguration));
            configurations.add(config);
        }
        // --disable=<service-name>
        for (String serviceName : args.getOptionValues("disable")) {
            OverriddenConfiguration config = new OverriddenConfiguration();
            config.setName(serviceName);
            configurations.remove(config);
        }

        // Start processes for each service configuration
        List<Process> processes = configurations.parallelStream()
                .map(this::constructServiceProcess)
                .map((pb) -> {
                    try {
                        Process process = pb.start();
                        LOGGER.info(String.format("Started process %s", process.toString()));
                        return process;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull).collect(Collectors.toList());

        // Wait for retcodes
        List<Integer> retCodes = processes.stream().parallel()
                .map((p) -> {
                    try {
                        return p.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull).collect(Collectors.toList());
        LOGGER.info(retCodes.toString());
    }

    ProcessBuilder constructServiceProcess(final OverriddenConfiguration config) {
        String[] command = new String[] {
                JavaEnvUtils.getJreExecutable("java"),
                "-jar", config.getTarget(),
                String.format("--spring-config-location=%s", config.getConfig())
        };
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectOutput(new File(config.getLog()));
        builder.redirectError(new File(config.getLog()));
        LOGGER.info(String.format("Constructed ProcessBuilder %s [%s]", Arrays.toString(command), builder.redirectOutput().toString()));
        return builder;
    }
}
