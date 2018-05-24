package util;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.math.IntMath.pow;

public class ChargingPointMonitor {
    private List<List<ChargingPointMeasurement>> measurements;
    private static final int AGG_SIZE = 50;

    public ChargingPointMonitor() {
        measurements = new ArrayList<>();
        measurements.add(new ArrayList<>());
    }

    public void addMeasurement(ChargingPointMeasurement measurement) {
        measurements.get(0).add(measurement);

        if (measurements.get(0).size() == AGG_SIZE) {
            aggregateMeasurements(0);
        }
    }

    private void aggregateMeasurements(int listIndex) {
        Tuple<Double, Double> aggregatedPercentages = new Tuple<>(0.0, 0.0);

        for (ChargingPointMeasurement measurement : measurements.get(listIndex)) {
            aggregatedPercentages.first += measurement.getOccupationLW();
            aggregatedPercentages.second += measurement.getOccupationHW();
        }

        aggregatedPercentages.first /= AGG_SIZE;
        aggregatedPercentages.second /= AGG_SIZE;

        if (measurements.size() < listIndex + 2) {
            measurements.add(new ArrayList<>());
        }

        measurements.get(listIndex+1).add(
            new ChargingPointMeasurement(aggregatedPercentages.first, aggregatedPercentages.second));
        measurements.get(listIndex).clear();

        if (measurements.get(listIndex+1).size() == AGG_SIZE) {
            // recursively aggregate next level
            aggregateMeasurements(listIndex+1);
        }
    }


    public ChargingPointMeasurement getAverageOccupation() {
        Tuple<Double, Double> aggregatedPercentages = new Tuple<>(0.0, 0.0);
        int totalAmountOfMeasurements = 0;

        for (int i = 0; i < measurements.size(); i++) {
            for (int j = 0; j < measurements.get(i).size(); j++) {
                ChargingPointMeasurement measurement = measurements.get(i).get(j);
                int amtMeasurementsToAdd = pow(AGG_SIZE,i);
                totalAmountOfMeasurements += amtMeasurementsToAdd;
                aggregatedPercentages.first += measurement.getOccupationLW() * amtMeasurementsToAdd;
                aggregatedPercentages.second += measurement.getOccupationHW() * amtMeasurementsToAdd;
            }
        }

        return new ChargingPointMeasurement(
aggregatedPercentages.first / totalAmountOfMeasurements,
aggregatedPercentages.second / totalAmountOfMeasurements);
    }
}
