package util;

public class ChargingPointMeasurement {
    private double occupationLW;
    private double occupationHW;

    public ChargingPointMeasurement(double occupationLW, double occupationHW) {
        this.occupationLW = occupationLW;
        this.occupationHW = occupationHW;
    }

    public double getOccupationHW() {
        return occupationHW;
    }

    public double getOccupationLW() {
        return occupationLW;
    }

    public String toString() {
        return "<Lightweight: " + occupationLW*100 + "%, Heavyweight: " + occupationHW*100 + "%>";
    }
}
