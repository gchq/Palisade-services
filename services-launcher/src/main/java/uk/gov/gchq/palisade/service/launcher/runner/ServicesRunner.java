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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ServicesRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesRunner.class);

    @Autowired
    List<ProcessBuilder> processBuilders;

    //@Autowired
    //DefaultsConfiguration defaultsConfiguration;

    //@Autowired
    //ServicesConfiguration servicesConfiguration;

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        for (String serviceName : args.getOptionNames()) {
        }
        List<Process> processes = processBuilders.stream().parallel()
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
}
