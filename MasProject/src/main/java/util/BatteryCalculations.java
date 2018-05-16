package util;

import com.github.rinde.rinsim.core.model.pdp.Drone;

public class BatteryCalculations {

    public static double calculateNecessaryBatteryLevel(Drone drone, double distancePickup,
                                                        double distanceDeliver, double distanceCharge,
                                                        double payloadCapacity) {
        // battery level is expressed in amount of seconds
        double necessaryBatteryLevel = 0;

        // The drone flies at full speed to the pickup location
        necessaryBatteryLevel += distancePickup / drone.getDTO().getSpeed();

        // After the pickup, the drone flies at linear speed dependent on the payload
        necessaryBatteryLevel += distanceDeliver / drone.getSpeed(payloadCapacity);

        // Calculate the battery level needed to reach the charging point after delivering the package
        necessaryBatteryLevel += distanceCharge / drone.getDTO().getSpeed();

        return necessaryBatteryLevel;
    }
}