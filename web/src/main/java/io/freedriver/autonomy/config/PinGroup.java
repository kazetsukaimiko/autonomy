package io.freedriver.autonomy.config;

import java.util.List;
import java.util.Map;

public class PinGroup {
    private String name;
    private List<Map<String, Boolean>> permutations;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Map<String, Boolean>> getPermutations() {
        return permutations;
    }

    public void setPermutations(List<Map<String, Boolean>> permutations) {
        this.permutations = permutations;
    }
}
