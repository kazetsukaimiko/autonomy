package io.freedriver.autonomy.cdi.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.autonomy.config.Configuration;
import io.freedriver.autonomy.config.PinGroup;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigurationProvider {
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/autonomy");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Produces @Default
    public Configuration getConfiguration() throws IOException {
        Path configFile = inConfigDirectory("config.json");
        if (!configFile.toFile().exists()) {
            OBJECT_MAPPER.writeValue(configFile.toFile(), generateFromMappings());
        }
        return OBJECT_MAPPER.readValue(configFile.toFile(), Configuration.class);
    }

    public static Path inConfigDirectory(String name) {
        return Paths.get(CONFIG_PATH.toAbsolutePath().toString(), name);
    }

    public static Configuration generateFromMappings() throws IOException {
        Path mappingsFile = inConfigDirectory("mappings.json");
        Map<Integer, String> mappings = OBJECT_MAPPER.readValue(mappingsFile.toFile(), new TypeReference<HashMap<Integer, String>>(){});

        Configuration configuration = new Configuration();
        configuration.setAliases(mappings);

        for (Integer pinNumber : mappings.keySet()) {
            String[] nameSplit = Optional.of(pinNumber)
                    .map(mappings::get)
                    //.filter(Objects::nonNull)
                    .filter(s -> !s.isBlank())
                    .map(s -> s.split("_"))
                    .orElse(new String[]{});
            if (nameSplit.length>0) {
                if (nameSplit.length>1) {
                    String group = nameSplit[0];
                    Set<Integer> members = mappings.entrySet()
                        .stream()
                        .filter(e -> e.getValue().startsWith(group + "_"))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());

                    configuration.getGroups().add(ofMappings(mappings, group, members));
                } else { // Its own zone.
                    configuration.getGroups().add(ofMappings(mappings, nameSplit[0], Collections.singleton(pinNumber)));
                }
            }
        }
        return configuration;
    }

    private static PinGroup ofMappings(Map<Integer, String> mappings, String group, Set<Integer> members) {
        PinGroup pinGroup = new PinGroup();
        pinGroup.setName(group);

        pinGroup.setPermutations(powerSet(members).stream()
                .map(on -> members.stream()
                        .collect(Collectors.toMap(
                                mappings::get,
                                on::contains)))
                .collect(Collectors.toList()));

        return pinGroup;
    }

    /**
     * Adaptation of https://stackoverflow.com/questions/4640034/calculating-all-of-the-subsets-of-a-set-of-numbers
     */
    public static <T> Set<Set<T>> powerSet(Collection<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

}
