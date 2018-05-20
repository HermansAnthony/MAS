import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.examples.experiment.ExamplePostProcessor;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.*;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;
import energy.ChargingPoint;
import energy.DefaultEnergyModel;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.graphics.RGB;
import org.jetbrains.annotations.NotNull;
import pdp.*;
import util.Range;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.measure.unit.SI;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class DroneExperiment {
    // Store related properties
    private static Point storeLocation1 = new Point(600, 600);
    private static Point storeLocation2 = new Point(300,300);

    // Map related properties
    private static Point resolution = new Point(1120.0,956.0);

    // Drone related properties
    private static final Range speedDroneLW = new Range(17,22);
    private static final Range speedDroneHW = new Range(11,22);

    private static int capacityDroneLW = 3500; // Expressed in grams
    private static int capacityDroneHW = 9000; // Expressed in grams

    private static int batteryDroneLW = 2400;  // Expressed in seconds
    private static int batteryDroneHW = 1500;  // Expressed in seconds

    //    private static final int droneRadius = 1;
    private static final int amountChargersLW = 5;
    private static final int amountChargersHW = 5;
    //    private static final int amountRequests = 100;
    private static final double orderProbability = 0.005;
    private static final int serviceDuration = 60000;
    private static final int maxCapacity = 9000;

    private static final Point chargingPointLocation = new Point(2500,2500);

    // Variable settings for scenario
    // TODO change this
    private static final long M1 = 60 * 1000L;
    private static final long M4 = 4 * 60 * 1000L;
    private static final long M5 = 5 * 60 * 1000L;
    private static final long M7 = 7 * 60 * 1000L;
    private static final long M10 = 10 * 60 * 1000L;
    private static final long M12 = 12 * 60 * 1000L;
    private static final long M13 = 13 * 60 * 1000L;
    private static final long M18 = 18 * 60 * 1000L;
    private static final long M20 = 20 * 60 * 1000L;
    private static final long M25 = 25 * 60 * 1000L;
    private static final long M30 = 30 * 60 * 1000L;
    private static final long M40 = 40 * 60 * 1000L;
    private static final long M60 = 60 * 60 * 1000L;
    private static final Point P1_PICKUP = new Point(100, 200);
    private static final Point P1_DELIVERY = new Point(400, 200);
    private static final Point P2_PICKUP = new Point(100, 100);
    private static final Point P2_DELIVERY = new Point(400, 100);

    private DroneExperiment() {}

    /**
     * Main method
     */
    public static void main(String[] args) {
//        final Optional<ExperimentResults> results;
        System.out.println("Creating the scenario...");
        Scenario scenario = createScenario();
        System.out.println("Scenario done...");
        // Starts the experiment builder.
        ExperimentResults results = Experiment.builder()
                .addConfiguration(MASConfiguration.builder()
                // NOTE: this example uses 'namedHandler's for Depots and Parcels, while
                // very useful for debugging these should not be used in production as
                // these are not thread safe. Use the 'defaultHandler()' instead.
                .addEventHandler(AddDepotEvent.class, AddDepotEvent.namedHandler())
                .addEventHandler(AddParcelEvent.class, AddParcelEvent.namedHandler())
                // There is no default handle for vehicle events, here a non functioning
                // handler is added, it can be changed to add a custom vehicle to the
                // simulator.
                .addEventHandler(AddVehicleEvent.class, null) // TODO change this to our drones
                .addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
                // Note: if you multi-agent system requires the aid of a model (e.g.
                // CommModel) it can be added directly in the configuration. Models that
                // are only used for the solution side should not be added in the
                // scenario as they are not part of the problem.
                .addModel(StatsTracker.builder()).build())

                // Adds the newly constructed scenario to the experiment. Every
                // configuration will be run on every scenario.
                .addScenario(scenario)

                // The number of repetitions for each simulation. Each repetition will
                // have a unique random seed that is given to the simulator.
                .repeat(1)

                // The master random seed from which all random seeds for the
                // simulations will be drawn.
                .withRandomSeed(0)

                // The number of threads the experiment will use, this allows to run
                // several simulations in parallel. Note that when the GUI is used the
                // number of threads must be set to 1.
                .withThreads(1)

                // We add a post processor to the experiment. A post processor can read
                // the state of the simulator after it has finished. It can be used to
                // gather simulation results. The objects created by the post processor
                // end up in the ExperimentResults object that is returned by the
                // perform(..) method of Experiment.
                .usePostProcessor(new PostProcessor() {
                    @Override
                    public Object collectResults(Simulator sim, Experiment.SimArgs args) {
                        return null;
                    }

                    @Override
                    public FailureStrategy handleFailure(Exception e, Simulator sim, Experiment.SimArgs args) {
                        return null;
                    }
                })

                // Adds the GUI just like it is added to a Simulator object.

                .showGui(createGui(false))

                // Starts the experiment, but first reads the command-line arguments
                // that are specified for this application. By supplying the '-h' option
                // you can see an overview of the supported options.
                .perform();

//        if (results.isPresent()) {
//            for (final Experiment.SimulationResult sr : results.get().getResults()) {
//                // The SimulationResult contains all information about a specific
//                // simulation, the result object is the object created by the post
//                // processor, a String in this case.
//                System.out.println(
//                        sr.getSimArgs().getRandomSeed() + " " + sr.getResultObject());
//            }
//        } else {
//            throw new IllegalStateException("Experiment did not complete.");
//        }
    }

    /**
     * Defines a simple scenario with one depot, one vehicle and three parcels.
     * Note that a scenario is supposed to only contain problem specific
     * information it should (generally) not make any assumptions about the
     * algorithm(s) that are used to solve the problem.
     * @return A newly constructed scenario.
     */
    static Scenario createScenario() {
        // In essence a scenario is just a list of events. The events must implement
        // the TimedEvent interface. You are free to construct any object as a
        // TimedEvent but keep in mind that implementations should be immutable.
        return Scenario.builder()
                // Adds one depot.
                .addEvent(AddDepotEvent.create(-1, storeLocation1))
                .addEvent(AddDepotEvent.create(-1, storeLocation2))
                .addEvent(AddVehicleEvent.create(-1, new DroneLW(speedDroneLW, capacityDroneLW, batteryDroneLW, chargingPointLocation).getDTO()))
                // Two add parcel events are added. They are announced at different
                // times and have different time windows.
                .addEvent(
                        AddParcelEvent.create(Parcel.builder(P1_PICKUP, P1_DELIVERY)
                                .neededCapacity(0)
                                .orderAnnounceTime(M1)
                                .pickupTimeWindow(TimeWindow.create(M1, M20))
                                .deliveryTimeWindow(TimeWindow.create(M4, M30))
                                .buildDTO()))

                .addEvent(
                        AddParcelEvent.create(Parcel.builder(P2_PICKUP, P2_DELIVERY)
                                .neededCapacity(0)
                                .orderAnnounceTime(M5)
                                .pickupTimeWindow(TimeWindow.create(M10, M25))
                                .deliveryTimeWindow(
                                        TimeWindow.create(M20, M40))
                                .buildDTO()))

                // Signals the end of the scenario. Note that it is possible to stop the
                // simulation before or after this event is dispatched, that depends on
                // the stop condition (see below).
                .addEvent(TimeOutEvent.create(M60))
                .scenarioLength(M60)

                // Adds a plane road model as this is part of the problem
                .addModel(
                        PDPRoadModel.builder(
                                RoadModelBuilders.plane()
                                        .withMinPoint(new Point(0,0))
                                        .withMaxPoint(resolution)
                                        .withMaxSpeed(22)))

                // Adds the pdp model
                // TODO wth is tardy
                .addModel(
                        DefaultPDPModel.builder()
                                .withTimeWindowPolicy(TimeWindowPolicy.TimeWindowPolicies.TARDY_ALLOWED))

                // The stop condition indicates when the simulator should stop the
                // simulation. Typically this is the moment when all tasks are performed.
                // Custom stop conditions can be created by implementing the StopCondition
                // interface.
                .setStopCondition(StopConditions.or(
                        StatsStopConditions.timeOutEvent(),
                        StatsStopConditions.vehiclesDoneAndBackAtDepot()))
                .build();
    }

    // TODO avoid code duplication
    private static View.Builder createGui(boolean testing) {
        System.out.println("Creating view...");
        View.Builder view = View.builder()
                .with(PlaneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder()
                        .withColorAssociation(
                                Parcel.class, new RGB(200, 200, 200))
                        .withImageAssociation(
                                Depot.class, "/store-40.png")
//                        .withImageAssociation(
//                                ChargingPoint.class, "/chargingPoint-40.png")
                        .withImageAssociation(
                                DroneLW.class, "/droneLW-32.png"))
//                        .withImageAssociation(
//                                DroneHW.class, "/droneHW-32.png"))
                .with(DroneRenderer.builder())
                .with(MapRenderer.builder("target/classes/leuven.png"))
                .withResolution(new Double(resolution.x).intValue(), new Double(resolution.y).intValue())
                .withTitleAppendix("Drone Demo - WIP");

        if (testing) {
            view = view.withAutoClose()
                    .withAutoPlay()
                    .withSimulatorEndTime(10000)
                    .withSpeedUp(64);
        }
        System.out.println("View done");
        return view;
    }
//    enum CustomVehicleHandler implements TimedEventHandler<AddVehicleEvent> {
//        INSTANCE {
//            @Override
//            public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
//                // add your own vehicle to the simulator here
//                sim.register(
//                        new ExampleRouteFollowingVehicle(event.getVehicleDTO(), true));
//            }
//        }
//    }
}
