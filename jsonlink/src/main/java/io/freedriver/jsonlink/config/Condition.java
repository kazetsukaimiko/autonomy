package io.freedriver.jsonlink.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
public abstract class Condition {
    private final ConditionType conditionType;

    protected Condition(ConditionType conditionType) {
        this.conditionType = conditionType;
    }
}
