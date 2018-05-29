package experiment;

import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
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
import com.github.rinde.rinsim.util.TimeWindow;
import energy.ChargingPoint;
import energy.DefaultEnergyModel;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import pdp.Customer;
import pdp.DroneHW;
import pdp.DroneLW;
import pdp.Order;
import renderer.ChargingPointPanel;
import renderer.DroneRenderer;
import renderer.MapRenderer;
import util.Range;
import util.Tuple;
import util.Utilities;

import javax.measure.unit.SI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DroneExperiment {
    private static final long TICK_LENGTH = 250;
    // Store related properties
    private static List<Point> storeLocations;

    // Map related properties
    private static Point resolutionImage = new Point(1120.0,956.0);

    // Drone related properties
    private static final Range speedDroneLW = new Range(17,22);
    private static final Range speedDroneHW = new Range(11,22);

    private static final int capacityDroneLW = 3500; // Expressed in grams
    private static final int capacityDroneHW = 9000; // Expressed in grams

    private static final int batteryDroneLW = 2400;  // Expressed in seconds
    private static final int batteryDroneHW = 1500;  // Expressed in seconds

    //    private static final int amountRequests = 100;
    private static final double orderProbability = 0.005;
    private static final int serviceDuration = 60000;
    private static final int maxCapacity = 9000;

    private static final Point chargingPointLocation = new Point(2500,2500);
    private static final long simulationLength = 1000000 * 10; // TODO adjust this to amount of ticks, and extend simulation duration (currently 16:40 minutes)


    private static final int SEED_ORDERS = 0;
    private static final int MAX_X = 5000;
    private static final int MAX_Y = 5000;
    private static final String map = "/leuven2_800x800.png";
//    private static final String map = "/leuven828.png";

    private static Map<Order, Tuple<Long, Long>> ordersInformation;

    /**
     * Main method
     */
    public static void main(String[] args) {
        storeLocations = Utilities.loadStoreLocations("/stores.csv");
        ordersInformation = new HashMap<>();
        Scenario scenario = null;
        String scenarioIdentifier = "Default";
        if (args.length >= 1)
            scenarioIdentifier = String.valueOf(args[0]);
        if (scenarioIdentifier.equals("Default"))
            scenario = createScenario(10, 20,5,5);
        if (scenarioIdentifier.equals("LW"))
            scenario = createScenario(30,0, 10,0);
        if (scenarioIdentifier.equals("HW"))
            scenario = createScenario(0,30, 0, 10);
        ExperimentResults results = Experiment.builder()
            .addConfiguration(MASConfiguration.builder()
                .addEventHandler(AddDepotEvent.class, AddDepotEvent.namedHandler())
                .addEventHandler(AddChargingPointEvent.class, AddChargingPointEvent.defaultHandler())
                .addEventHandler(AddOrderEvent.class, AddOrderEvent.defaultHandler())
                .addEventHandler(AddDroneEvent.class, AddDroneEvent.defaultHandler())
                .addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
                // Note: if you multi-agent system requires the aid of a model (e.g.
                // CommModel) it can be added directly in the configuration. Models that
                // are only used for the solution side should not be added in the
                // scenario as they are not part of the problem.
                .addModel(StatsTracker.builder())
                .addModel(TimeModel.builder().withTickLength(TICK_LENGTH)).build())
            .addScenario(scenario)
            .repeat(1)
            .showGui(false)
            .withRandomSeed(0)
            .withThreads(1)
            .usePostProcessor(new ExperimentPostProcessor())
            .showGui(createGui(false, "Scenario: " + scenarioIdentifier))
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
     * Creates a new scenario
     * @param amountDroneLW the amount of lightweight drones used in the experiment
     * @param amountDroneHW the amount of heavyweight drones used in the experiment
     * @return A newly constructed scenario.
     */
    static Scenario createScenario(int amountDroneLW, int amountDroneHW, int amountChargersLW, int amountChargersHW) {
        Scenario.Builder scenarioBuilder = Scenario.builder();
        // Creation of all objects for the scenario
        RandomGenerator rng = new MersenneTwister();
        rng.setSeed(SEED_ORDERS);
        for (int i = 0; i < simulationLength; i+=TICK_LENGTH) {
            if (rng.nextDouble() < orderProbability) {
                Point location = new Point(rng.nextDouble() * MAX_X, rng.nextDouble() * MAX_Y);
                int randomStore = rng.nextInt(storeLocations.size());
                int capacity = 1000 + rng.nextInt(maxCapacity - 1000);
                // if a scenario only contains the
                if (capacity > capacityDroneLW && amountDroneHW == 0)
                    generateDifferentParcels(scenarioBuilder, i, capacity, randomStore, location);
                if (amountDroneHW != 0) {
                    ParcelDTO orderData = Parcel.builder(storeLocations.get(randomStore), location)
                        .serviceDuration(serviceDuration)
                        .neededCapacity(capacity) // Capacity is measured in grams
                        .deliveryDuration(5)
                        .pickupDuration(5)
                        .orderAnnounceTime(i)
                        .pickupTimeWindow(TimeWindow.create(i, i + 1000000)) // TODO verify/fine tuning
                        .deliveryTimeWindow(TimeWindow.create(i+1000000, i+1500000))
                        .buildDTO();
                    scenarioBuilder.addEvent(AddOrderEvent.create(orderData));
                }
            }
        }
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

        // Addition of models
        scenarioBuilder.addModel(RoadModelBuilders.plane()
                .withMinPoint(new Point(0,0))
                .withMaxPoint(new Point(MAX_X,MAX_Y))
                .withDistanceUnit(SI.METER)
                .withSpeedUnit(SI.METERS_PER_SECOND)
                .withMaxSpeed(50))
            // TODO figure out what the different time windows are
            .addModel(DefaultPDPModel.builder().withTimeWindowPolicy(TimeWindowPolicy.TimeWindowPolicies.LIBERAL))
            .addModel(DefaultEnergyModel.builder());

        // Time and timeouts of scenario
        scenarioBuilder.scenarioLength(simulationLength)
            .addEvent(TimeOutEvent.create(simulationLength))
            .setStopCondition(StatsStopConditions.timeOutEvent());

        return scenarioBuilder.build();
    }

    private static void generateDifferentParcels(Scenario.Builder scenarioBuilder, int time, int capacity, int randomStore, Point location){
        int orderWeight1 = capacityDroneLW;
        int orderWeight2 = capacity - orderWeight1;
        int orderWeight3 = 0;
        if ( orderWeight2 > capacityDroneLW){
            orderWeight3 = orderWeight2 - capacityDroneLW;
            orderWeight2 = capacityDroneLW;
        }

        // First part of the order
        ParcelDTO order1 = Parcel.builder(storeLocations.get(randomStore), location)
            .serviceDuration(serviceDuration)
            .neededCapacity(orderWeight1) // Capacity is measured in grams
            .deliveryDuration(5)
            .pickupDuration(5)
            .orderAnnounceTime(time)
            .pickupTimeWindow(TimeWindow.create(time, time + 1000000)) // TODO verify/fine tuning
            .deliveryTimeWindow(TimeWindow.create(time+1000000, time+1500000))
            .buildDTO();
        scenarioBuilder.addEvent(AddOrderEvent.create(order1));

        // Second part of the order
        ParcelDTO order2 = Parcel.builder(storeLocations.get(randomStore), location)
            .serviceDuration(serviceDuration)
            .neededCapacity(orderWeight2) // Capacity is measured in grams
            .deliveryDuration(5)
            .pickupDuration(5)
            .orderAnnounceTime(time)
            .pickupTimeWindow(TimeWindow.create(time, time + 1000000)) // TODO verify/fine tuning
            .deliveryTimeWindow(TimeWindow.create(time+1000000, time+1500000))
            .buildDTO();
        scenarioBuilder.addEvent(AddOrderEvent.create(order2));

        // Possible third part of the order
        if (orderWeight3 != 0){
            ParcelDTO order3 = Parcel.builder(storeLocations.get(randomStore), location)
                .serviceDuration(serviceDuration)
                .neededCapacity(orderWeight1) // Capacity is measured in grams
                .deliveryDuration(5)
                .pickupDuration(5)
                .orderAnnounceTime(time)
                .pickupTimeWindow(TimeWindow.create(time, time + 1000000)) // TODO verify/fine tuning
                .deliveryTimeWindow(TimeWindow.create(time+1000000, time+1500000))
                .buildDTO();
            scenarioBuilder.addEvent(AddOrderEvent.create(order3));
        }
    }

    private static View.Builder createGui(boolean testing, String name) {
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
            .with(MapRenderer.builder(map))
            .with(TimeLinePanel.builder())
            .with(StatsPanel.builder())
            .with(ChargingPointPanel.builder())
            .withResolution(new Double(resolutionImage.x).intValue(), new Double(resolutionImage.y).intValue())
            .withTitleAppendix(name);

        if (testing) {
            view = view.withAutoClose()
                .withAutoPlay()
                .withSimulatorEndTime(10000)
                .withSpeedUp(64);
        }
        return view;
    }

}
