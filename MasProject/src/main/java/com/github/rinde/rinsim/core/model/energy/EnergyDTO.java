package com.github.rinde.rinsim.core.model.energy;

import static java.lang.Math.min;

public class EnergyDTO {
    final int MAX_CAPACITY;
    int batteryLevel;

    public EnergyDTO(int _batteryLevel) {
        batteryLevel = _batteryLevel;
        MAX_CAPACITY = batteryLevel;
    }


    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void decreaseBatteryLevel(int amount) {
//        assert(batteryLevel - amount >= 0); TODO uncomment
        batteryLevel -= amount;
    }

    public void recharge(int amount) {
        batteryLevel = min(batteryLevel + amount, MAX_CAPACITY);
    }

    public boolean fullyCharged() {
        return MAX_CAPACITY == batteryLevel;
    }
}


