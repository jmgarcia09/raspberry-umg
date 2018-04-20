package com.umg.raspberry;

import com.pi4j.io.gpio.PinState;

import java.util.List;

public class MotorPin {

    private String pinNumber;
    private List<PinState> pinStates;


    public MotorPin(String pinNumber, List<PinState> pinStates) {
        this.pinNumber = pinNumber;
        this.pinStates = pinStates;
    }

    public String getPinNumber() {
        return pinNumber;
    }

    public void setPinNumber(String pinNumber) {
        this.pinNumber = pinNumber;
    }

    public List<PinState> getPinStates() {
        return pinStates;
    }

    public void setPinStates(List<PinState> pinStates) {
        this.pinStates = pinStates;
    }
}
