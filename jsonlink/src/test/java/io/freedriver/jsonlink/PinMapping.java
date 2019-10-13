package io.freedriver.jsonlink;

public class PinMapping {
    private int pinNumber;
    private String name;

    public PinMapping() {
    }

    public PinMapping(int pinNumber, String name) {
        this.pinNumber = pinNumber;
        this.name = name;
    }

    public PinMapping(String entry) {
    }

    public int getPinNumber() {
        return pinNumber;
    }

    public void setPinNumber(int pinNumber) {
        this.pinNumber = pinNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
