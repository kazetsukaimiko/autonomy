package io.freedriver.autonomy.cdi.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.jsonlink.config.ConnectorConfig;
import io.freedriver.jsonlink.config.ConnectorConfigs;
import io.freedriver.jsonlink.config.Mappings;
import io.freedriver.jsonlink.config.PinName;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ConfigurationProvider {
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".config/autonomy");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Produces @Default
    public ConnectorConfig getConfiguration() throws IOException {
        if (true) {
            return new ConnectorConfig();
        }
        Path configFile = inConfigDirectory("config.json");
        if (!configFile.toFile().exists()) {
            OBJECT_MAPPER.writeValue(configFile.toFile(), generateFromMappings());
        }
        return OBJECT_MAPPER.readValue(configFile.toFile(), ConnectorConfig.class);
    }

    public static Path inConfigDirectory(String name) {
        return Paths.get(CONFIG_PATH.toAbsolutePath().toString(), name);
    }

    public static ConnectorConfigs generateFromMappings() throws IOException {
        Path mappingsFile = inConfigDirectory("mappings.json");
        Mappings mappings = OBJECT_MAPPER.readValue(mappingsFile.toFile(), Mappings.class);

        ConnectorConfigs configurations = new ConnectorConfigs();


        mappings.getMappings().forEach(mapping -> {
            ConnectorConfig configuration = new ConnectorConfig();
            configurations.getConnectors().put(mapping.getConnectorId(), configuration);
            mapping.getPinNamesAsEntities()
                    .stream()
                    .map(PinName::getGroup)
                    .forEach(groupName -> {
                        //Permutation pinGroup = configuration.getGroups().getOrDefault(groupName, new Permutation());
                        //configuration.getGroups().put(groupName, pinGroup);
                    });
        });


        //configuration.setPinNames(mappings);


        // TODO: CRUD
        /*


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

         */


        return configurations;
    }

/*
    private static Set<Permutation> ofMappings(Set<PinName> pinNames) {
        return pinNames.stream()
                .map(PinName::getGroup)
                .map(groupName -> ofMappings(groupName, pinNames.stream().filter(p -> p.ofGroup(groupName)).collect(Collectors.toSet())))
                .collect(Collectors.toSet());
    }

    private static Permutation ofMappings(String group, Set<PinName> pinNamesOfGroup) {
        Map<Integer, String> remap = pinNamesOfGroup.stream()
                .collect(Collectors.toMap(
                        pinName -> pinName.getPinNumber().getPin(),
                        PinName::getPinName,
                        (a, b) -> b
                        ));
        return ofMappings(remap, group, pinNamesOfGroup.stream().map(pinName -> pinName.getPinNumber().getPin()).collect(Collectors.toSet()));
    }

    private static Permutation ofMappings(Map<Integer, String> mappings, String group, Set<Integer> members) {
        Permutation pinGroup = new Permutation();

        pinGroup.setPermutations(powerSet(members).stream()
                .map(on -> members.stream()
                        .collect(Collectors.toMap(
                                mappings::get,
                                on::contains)))
                .collect(Collectors.toList()));

        return pinGroup;
    }*/

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
