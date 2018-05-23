package experiment;

import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.*;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import energy.ChargingPoint;
import energy.DefaultEnergyModel;
import pdp.Customer;
import pdp.DroneHW;
import pdp.DroneLW;
import renderer.ChargingPointPanel;
import renderer.DroneRenderer;
import renderer.MapRenderer;
import util.Range;

import javax.measure.unit.SI;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class DroneExperiment {
    // Store related properties
    private static List<Point> storeLocations;

    // Map related properties
    private static Point resolution = new Point(1120.0,956.0);

    // Drone related properties
    private static final Range speedDroneLW = new Range(17,22);
    private static final Range speedDroneHW = new Range(11,22);

    private static final int capacityDroneLW = 3500; // Expressed in grams
    private static final int capacityDroneHW = 9000; // Expressed in grams

    private static final int batteryDroneLW = 2400;  // Expressed in seconds
    private static final int batteryDroneHW = 1500;  // Expressed in seconds

    private static final int amountDroneLW = 1;
    private static final int amountDroneHW = 1;

    //    private static final int droneRadius = 1;
    private static final int amountChargersLW = 5;
    private static final int amountChargersHW = 5;
    //    private static final int amountRequests = 100;
    private static final double orderProbability = 0.005;
    private static final int serviceDuration = 60000;
    private static final int maxCapacity = 9000;

    private static final Point chargingPointLocation = new Point(2500,2500);
    private static final long simulationLength = 100000;

    private DroneExperiment() {}

    /**
     * Main method
     */
    public static void main(String[] args) {
        loadStoreLocations("src/main/resources/stores.csv");
        Scenario scenario = createScenario();

        ExperimentResults results = Experiment.builder()
            .addConfiguration(MASConfiguration.builder()
                .addEventHandler(AddDepotEvent.class, AddDepotEvent.namedHandler())
                .addEventHandler(AddChargingPointEvent.class, AddChargingPointEvent.defaultHandler())
                .addEventHandler(AddParcelEvent.class, AddOrderEvent.defaultHandler())
                .addEventHandler(AddDroneEvent.class, AddDroneEvent.defaultHandler())
                .addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
                // Note: if you multi-agent system requires the aid of a model (e.g.
                // CommModel) it can be added directly in the configuration. Models that
                // are only used for the solution side should not be added in the
                // scenario as they are not part of the problem.
                .addModel(StatsTracker.builder()).build())
            .addScenario(scenario)
            .repeat(1)
            .withRandomSeed(0)
            .withThreads(1)
            .usePostProcessor(new ExperimentPostProcessor())
            .showGui(createGui(false))
            .perform();

        try {
            for (final Experiment.SimulationResult sr : results.getResults()) {
                // The SimulationResult contains all information about a specific
                // simulation, the result object is the object created by the post
                // processor, a String in this case.
                System.out.println(sr.getResultObject());
            }
        } catch(Exception e) {
            throw new IllegalStateException("Experiment did not complete.");
        }
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

        Scenario.Builder scenarioBuilder = Scenario.builder();

        // TODO manually generate order events and add them to the scenario?
//        ScenarioGenerator generator = ScenarioGenerator.builder()
//            .parcels(OrderGenerator.builder()
//                .pickupDurations(StochasticSuppliers.constant(5L))
//                .deliveryDurations(StochasticSuppliers.constant(5L))
//                .locations(Locations.builder().buildFixed(storeLocations))
//                .neededCapacities(StochasticSuppliers.uniformInt(1000, 9000))
//                .serviceDurations(StochasticSuppliers.uniformLong(10000,60000)) // TODO fill in these values more thoroughly
//                .announceTimes(TimeSeries.uniform(simulationLength, 10000, 20)) // TODO experimental values
////                .timeWindows()
//                .build())
//            .build();


        for (Point location : storeLocations) {
            scenarioBuilder.addEvent(AddDepotEvent.create(-1, location));
        }

        for (int i = 0; i < amountDroneLW; i++) {
            scenarioBuilder.addEvent(AddDroneEvent
                .create(new DroneLW(speedDroneLW, capacityDroneLW, batteryDroneLW, chargingPointLocation)));
        }

        for (int i = 0; i < amountDroneHW; i++) {
            scenarioBuilder.addEvent(AddDroneEvent
                .create(new DroneHW(speedDroneHW, capacityDroneHW, batteryDroneHW, chargingPointLocation)));
        }

        scenarioBuilder.addEvent(AddChargingPointEvent.create(chargingPointLocation, amountChargersLW, amountChargersHW));

        return scenarioBuilder
            .addEvent(TimeOutEvent.create(simulationLength))
            .scenarioLength(simulationLength)
            .addModel(RoadModelBuilders.plane()
                .withMinPoint(new Point(0,0))
                .withMaxPoint(new Point(5000,5000))
                .withDistanceUnit(SI.METER)
                .withSpeedUnit(SI.METERS_PER_SECOND)
                .withMaxSpeed(50))
            .addModel(DefaultPDPModel.builder())
            .addModel(DefaultEnergyModel.builder())
            .setStopCondition(StatsStopConditions.timeOutEvent())
            .build();
    }

    // TODO avoid code duplication
    private static View.Builder createGui(boolean testing) {
        View.Builder view = View.builder()
                .with(PlaneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(
                                Customer.class, "/customer-32.png")
                        .withImageAssociation(
                                Depot.class, "/store-40.png")
                        .withImageAssociation(
                                ChargingPoint.class, "/chargingPoint-40.png")
                        .withImageAssociation(
                                DroneLW.class, "/droneLW-32.png")
                        .withImageAssociation(
                                DroneHW.class, "/droneHW-32.png"))
                .with(DroneRenderer.builder())
                .with(MapRenderer.builder("target/classes/leuven.png"))
                .with(TimeLinePanel.builder())
                .with(RouteRenderer.builder())
//                .with(RoutePanel.builder().withPositionLeft())
//                .with(StatsPanel.builder())
                .with(ChargingPointPanel.builder())
                .withResolution(new Double(resolution.x).intValue(), new Double(resolution.y).intValue())
                .withTitleAppendix("Drone experiment - WIP");

        if (testing) {
            view = view.withAutoClose()
                .withAutoPlay()
                .withSimulatorEndTime(10000)
                .withSpeedUp(64);
        }
        return view;
    }


    /**
     * TODO duplicated from DroneExample
     * Reads the store locations from the specified csv file.
     * @param filename the csv file.
     */
    private static void loadStoreLocations(String filename) {
        storeLocations = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File(filename));
            scanner.useDelimiter("[,\n]");
            while (scanner.hasNext()) {
                storeLocations.add(new Point(new Double(scanner.next()), new Double(scanner.next())));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not read csv file with store locations.");
        }
    }
}
