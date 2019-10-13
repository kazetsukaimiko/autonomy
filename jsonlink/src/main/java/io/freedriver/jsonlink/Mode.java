package io.freedriver.jsonlink;

public enum Mode {
    INPUT(false),
    OUTPUT(true);

    private final boolean modeValue;

    private Mode(boolean modeValue) {
        this.modeValue = modeValue;
    }

    public boolean getModeValue() {
        return modeValue;
    }

    public static Mode fromBoolean(boolean modeValue) {
        return modeValue ? INPUT : OUTPUT;
    }
}
