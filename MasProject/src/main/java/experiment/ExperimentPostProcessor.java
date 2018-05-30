package experiment;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.PostProcessor;
import energy.EnergyModel;
import pdp.DroneHW;
import pdp.DroneLW;
import util.Tuple;
import util.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * This class is responsible for extracting the information needed to answer our research questions.
 * @author Anthony Hermans, Federico Quin
 */
public final class ExperimentPostProcessor implements PostProcessor<String> {

    ExperimentPostProcessor() {}

    @Override
    public String collectResults(Simulator sim, SimArgs args) {
        // Read state of simulator, check how many light weight drones exist in the road model
        final Set<DroneLW> dronesLW = sim.getModelProvider()
                .getModel(PlaneRoadModel.class).getObjectsOfType(DroneLW.class);
        final Set<DroneHW> dronesHW = sim.getModelProvider()
                .getModel(PlaneRoadModel.class).getObjectsOfType(DroneHW.class);

        EnergyModel em = sim.getModelProvider().getModel(EnergyModel.class);
        // Construct a result string based on the simulator state, of course, in
        // actual code the result should not be a string but a value object
        // containing the values of interest.
        final StringBuilder sb = new StringBuilder();
        if (dronesLW.isEmpty()) {
            sb.append("No lightweight drones were added\n");
        } else {
            sb.append(dronesLW.size()).append(" lightweight drones were added.\n");
        }
        if (dronesHW.isEmpty()) {
            sb.append("No heavyweight drones were added.\n");
        } else {
            sb.append(dronesHW.size()).append(" heavyweight drones were added.\n");
        }

        // The average occupation at the charging station
        sb.append("Average charging station occupation: ");
        sb.append(em.getChargingPoint().getAverageOccupation().toString()).append(".\n");

        // The average order delivery time
        sb.append("Average delivery time: ");
        Tuple<String, Integer> infoOnTime = getAverageTime("logging/ordersOnTime.csv");
        sb.append(infoOnTime.first).append(".\n");

        // The average overdue time
        sb.append("Average time overdue: ");
        Tuple<String, Integer> infoOverdue = getAverageTime("logging/ordersOverdue.csv");
        sb.append(infoOverdue.first).append(".\n");

        // Information relating the order amounts
        sb.append("Amount of delivered orders: ").append(infoOnTime.second).append(".\n");
        sb.append("Amount of overdue orders: ").append(infoOverdue.second).append(".\n");
        sb.append("Order delivery percentage: ");
        int totalOrders = infoOnTime.second+infoOverdue.second;
        if (totalOrders == 0) totalOrders = 1;
        int orderPercentage = (infoOnTime.second/totalOrders) * 100;
        sb.append(orderPercentage).append("%.\n");

        if (sim.getCurrentTime() >= args.getScenario().getTimeWindow().end()) {
            sb.append("Simulation has completed.");
        } else {
            sb.append("Simulation was stopped prematurely.");
        }
        PlaneRoadModel roadModel = sim.getModelProvider().getModel(PlaneRoadModel.class);

//        for (DroneLW u: dronesLW) {
//            System.err.println("Unregister LW drone)");
//            sim.unregister(u);
//            roadModel.removeObject(u);
//            roadModel.unregister(u);
//            pdp.unregister(u);
//        }
//        for(DroneHW u: dronesHW){
//            System.err.println("Unregister HW drone)");
//            sim.unregister(u);
//            roadModel.removeObject(u);
//            roadModel.unregister(u);
//            pdp.unregister(u);
//        }
        return sb.toString();
    }

    @Override
    public FailureStrategy handleFailure(Exception e, Simulator sim,
                                         SimArgs args) {
        // Signal that when an exception occurs the entire experiment should be
        // aborted.
        return FailureStrategy.ABORT_EXPERIMENT_RUN;
    }

    /**
     * Reads from a csv file and computes the average time (based of difference in the tuples)
     * @param fileName the csv file which contains the time related information
     * @return A tuple containing the average difference/latency and amount of parcels
     */
    private Tuple<String, Integer> getAverageTime(String fileName){
        List<Tuple<Long, Long>> ordersInfo = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File(fileName));
            scanner.useDelimiter("[,\n]");
            while (scanner.hasNext()) {
                ordersInfo.add(new Tuple<>(new Long(scanner.next()), new Long(scanner.next())));
            }
            scanner.close();
        } catch (IOException e) {
            System.out.println("File with name " + fileName + " does not exist.");
        }
        List<Long> differences = new ArrayList<>();
        for (Tuple<Long, Long> timeInfo: ordersInfo)
            differences.add(timeInfo.second - timeInfo.first);
        if (differences.isEmpty()) return new Tuple<>(Utilities.convertTimeToString(0),0);
        long averageDifference = differences.stream().mapToLong(Long::intValue).sum() / differences.size();
        return new Tuple<>(Utilities.convertTimeToString(averageDifference),ordersInfo.size());
    }
}
