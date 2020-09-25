package io.freedriver.autonomy.jaxrs.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AliasView {
    Map<String, Boolean> applianceStates = new LinkedHashMap<>();
    Map<String, Boolean> groupStates = new LinkedHashMap<>();
    Map<String, Set<String>> groups = new LinkedHashMap<>();

    public AliasView() {
    }

    public Map<String, Boolean> getApplianceStates() {
        return applianceStates;
    }

    public void setApplianceStates(Map<String, Boolean> applianceStates) {
        this.applianceStates = applianceStates;
    }

    public Map<String, Boolean> getGroupStates() {
        return groupStates;
    }

    public void setGroupStates(Map<String, Boolean> groupStates) {
        this.groupStates = groupStates;
    }

    public Map<String, Set<String>> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Set<String>> groups) {
        this.groups = groups;
    }
}
