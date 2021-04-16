/*
 * Copyright 2018-2021 Crown Copyright
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

package uk.gov.gchq.palisade.service.attributemask.stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValueFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.NonNull;

import uk.gov.gchq.palisade.service.attributemask.exception.PropertyLoadingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Parse and convert Spring maps and lists to Akka configs
 */
public class PropertiesConfigurer extends PropertySourcesPlaceholderConfigurer implements InitializingBean {
    private static final Pattern INDEXED_PROPERTY_PATTERN = Pattern.compile("^\\s*(?<path>\\w+(?:\\.\\w+)*)\\[(?<index>\\d+)\\]\\.*(.*?)$");
    private static final int PROPERTY_PATH = 1;
    private static final int PROPERTY_INDEX = 2;
    private static final int PROPERTY_TAIL = 3;
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile(".*\\]\\.(.*?)$");
    private static final String LIST_ITEM_SEPARATOR = ",";

    private String[] locations;
    private final ResourceLoader resourceLoader;
    private Environment environment;

    /**
     * Autowired constructor for a PropertiesConfigurer, requiring the Spring resourceLoader and environment
     * used when starting the application.
     * This will then convert the Spring YAML maps and lists to Akka HOCON format.
     *
     * @param resourceLoader the Spring application {@link ResourceLoader} for reading resources
     * @param environment    the Spring application {@link Environment} for reading active profiles and mutable property sources
     */
    public PropertiesConfigurer(final ResourceLoader resourceLoader, final Environment environment) {
        this.resourceLoader = resourceLoader;
        this.environment = environment;
    }

    @Override
    public void setEnvironment(final @NonNull Environment environment) {
        this.environment = environment;
        super.setEnvironment(environment);
    }

    @Override
    public void afterPropertiesSet() {
        MutablePropertySources envPropSources = ((ConfigurableEnvironment) this.environment).getPropertySources();
        envPropSources.forEach((PropertySource<?> propertySource) -> {
            if (propertySource.containsProperty("application.properties.locations")) {
                locations = ((String) propertySource.getProperty("application.properties.locations"))
                        .split(LIST_ITEM_SEPARATOR);
                Arrays.stream(locations)
                        .forEach(filename -> loadProperties(filename)
                                .forEach(envPropSources::addFirst));
            }
        });
    }

    private List<PropertySource<?>> loadProperties(final String filename) {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        try {
            final Resource[] possiblePropertiesResources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(filename);
            return Arrays.stream(possiblePropertiesResources)
                    .filter(Resource::exists)
                    .map((Resource resource) -> {
                        try {
                            return loader.load(resource.getFilename(), resource);
                        } catch (IOException e) {
                            String message = loader.getClass().getSimpleName() + " failed to load file " + resource.getFilename();
                            throw new PropertyLoadingException(message, e);
                        }
                    }).flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            String message = this.getClass().getSimpleName() + " failed to load file " + filename;
            throw new PropertyLoadingException(message, e);
        }
    }

    public Map<String, String> getAllActiveProperties() {
        return StreamSupport.stream(((AbstractEnvironment) environment).getPropertySources().spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource).map(EnumerablePropertySource.class::cast)
                .map(EnumerablePropertySource::getPropertyNames)
                .flatMap(Arrays::stream)
                .distinct()
                .filter(akka -> akka.startsWith("akka"))
                .collect(Collectors.toMap(Function.identity(), environment::getProperty));
    }

    /**
     * Convert a Spring key:value YAML-style map to an Akka Config
     *
     * @param props the property map to convert
     * @return an equivalent Akka config to the given properties, converting maps and arrays appropriately
     */
    public Config toHoconConfig(final Map<String, String> props) {
        final Map<String, String> std = props.entrySet().stream()
                .filter(entry -> !entry.getKey().matches(INDEXED_PROPERTY_PATTERN.pattern()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Map<String, String> array = props.entrySet().stream()
                .filter(entry -> entry.getKey().matches(INDEXED_PROPERTY_PATTERN.pattern()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Config config = ConfigFactory.parseMap(std);

        List<String> keys = array.keySet().stream()
                .map(PropertiesConfigurer::reductionKey)
                .distinct()
                .collect(Collectors.toList());
        for (String key : keys) {
            Map<String, String> values = array.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(key))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            config = toConfig(config, key, values);
        }
        return config;
    }

    private static Config toConfig(final Config orig, final String key, final Map<String, String> config) {
        // Map or list?
        Matcher mat = FIELD_NAME_PATTERN.matcher(config.keySet().stream().findFirst().orElse(""));
        if (mat.find()) {
            Map<String, String> node = new HashMap<>();
            config.forEach((String mapKey, String value) -> {
                Matcher fieldNameKeyMatcher = FIELD_NAME_PATTERN.matcher(mapKey);
                if (fieldNameKeyMatcher.matches()) {
                    node.put(fieldNameKeyMatcher.group(1), value);
                }
            });
            ConfigObject configItem = ConfigValueFactory.fromMap(node);
            ArrayList<ConfigObject> list = new ArrayList<>();
            list.add(configItem);
            return orig.withValue(key, ConfigValueFactory.fromIterable(list));
        } else {
            List<String> node = new ArrayList<>(config.values());
            ConfigList configList = ConfigValueFactory.fromIterable(node);
            return orig.withValue(key, configList);
        }
    }

    private static String reductionKey(final String key) {
        Matcher mat = INDEXED_PROPERTY_PATTERN.matcher(key);
        // Early return if this is a simple key/value property
        if (!mat.matches() || mat.groupCount() <= PROPERTY_INDEX) {
            return "";
        }
        // Parse the index if the property was a list
        String root = mat.group(PROPERTY_PATH);
        String index = mat.group(PROPERTY_INDEX);
        // Match on the tail (nested objects)
        mat = INDEXED_PROPERTY_PATTERN.matcher(mat.group(PROPERTY_TAIL));
        while (mat.matches()) {
            String prevIndex = mat.group(PROPERTY_INDEX);
            root = String.format("%s[%s].%s", root, index, mat.group(PROPERTY_PATH));
            // "Recursively" match on the tail until there are no more nested lists
            mat = INDEXED_PROPERTY_PATTERN.matcher(mat.group(PROPERTY_TAIL));
            if (mat.matches()) {
                if (mat.group(PROPERTY_PATH).isEmpty()) {
                    return root;
                } else {
                    // Re-insert the previous index into the root string
                    root = String.format("%s[%s].%s", root, prevIndex, mat.group(PROPERTY_PATH));
                    mat = INDEXED_PROPERTY_PATTERN.matcher(mat.group(PROPERTY_PATH));
                }
            }
        }
        return root;
    }

}
