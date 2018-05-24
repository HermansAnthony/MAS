package experiment;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.PostProcessor;
import energy.EnergyModel;
import pdp.DroneHW;
import pdp.DroneLW;

import java.util.Set;

/**
 * This is an example implementation of a {@link ExperimentPostProcessor}. In this example
 * the simulation result is a string.
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

        sb.append("The average occupation of the charging station of the simulation was ");
        sb.append(em.getChargingPoint().getAverageOccupation().toString()).append(".\n");

        if (sim.getCurrentTime() >= args.getScenario().getTimeWindow().end()) {
            sb.append("Simulation has completed.");
        } else {
            sb.append("Simulation was stopped prematurely.");
        }
        return sb.toString();
    }

    @Override
    public FailureStrategy handleFailure(Exception e, Simulator sim,
                                         SimArgs args) {
        // Signal that when an exception occurs the entire experiment should be
        // aborted.
        return FailureStrategy.ABORT_EXPERIMENT_RUN;
    }
}