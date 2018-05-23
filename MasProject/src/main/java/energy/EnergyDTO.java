package energy;

import static java.lang.Math.min;

public class EnergyDTO {
    final double MAX_CAPACITY;
    double batteryLevel;

    public EnergyDTO(double batteryLevel) {
        this.batteryLevel = batteryLevel;
        MAX_CAPACITY = this.batteryLevel;
    }


    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void decreaseBatteryLevel(double amount) {
        assert(batteryLevel - amount >= 0);
        batteryLevel -= amount;
    }

    public void recharge(double amount) {
        batteryLevel = min(batteryLevel + amount, MAX_CAPACITY);
    }

    public boolean fullyCharged() {
        return MAX_CAPACITY == batteryLevel;
    }

    public double getMaxCapacity() {
        return MAX_CAPACITY;
    }
}


