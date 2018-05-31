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
import renderer.DroneRenderer;
import renderer.MapRenderer;
import util.PropertiesLoader;
import util.Utilities;

import javax.measure.unit.SI;
import java.util.List;


public class DroneExperiment {
    private static List<Point> storeLocations;
    private static Point resolutionImage;

    private static final double orderProbability = 0.005;
    private static final int serviceDuration = 60000;

    private static final long simulationLength = 1000000 * 10; // TODO adjust this to amount of ticks, and extend simulation duration (currently 16:40 minutes)

    private static final int SEED_ORDERS = 0;

    private static PropertiesLoader propertiesLoader;

    /**
     * Main method
     */
    public static void run(String scenarioIdentifier) {
        propertiesLoader = PropertiesLoader.getInstance();
        storeLocations = Utilities.loadStoreLocations(propertiesLoader.getStoresLocation());
        resolutionImage = Utilities.loadResolutionImage(propertiesLoader.getMapLocation());
        Scenario scenario = null;

        if (scenarioIdentifier.equals("default"))
            scenario = createScenario(10, 20,5,5);
        if (scenarioIdentifier.equals("lw"))
            scenario = createScenario(30,0, 10,0);
        if (scenarioIdentifier.equals("hw"))
            scenario = createScenario(0,30, 0, 10);

        String name = "Scenario_" + scenarioIdentifier;
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
                .addModel(TimeModel.builder().withTickLength(propertiesLoader.getTickLength())).build())
            .addScenario(scenario)
            .repeat(Integer.valueOf(propertiesLoader.getProperty("Experiment.amtRepeat")))
            .showGui(createGui(false, name))
            .showGui(Boolean.valueOf(propertiesLoader.getProperty("Experiment.showGUI")))
            .withRandomSeed(Integer.valueOf(propertiesLoader.getProperty("Experiment.seed")))
            .withThreads(1)
            .usePostProcessor(new ExperimentPostProcessor(name))
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
        int tickLength = propertiesLoader.getTickLength();
        int mapSize = propertiesLoader.getMapSize();
        int minCapacity = Integer.valueOf(propertiesLoader.getProperty("Experiment.minCapacityOrder"));
        int maxCapacity = Integer.valueOf(propertiesLoader.getProperty("Experiment.maxCapacityOrder"));
        int capacityDroneLW = propertiesLoader.getCapacityLW();
        int capacityDroneHW = propertiesLoader.getCapacityHW();
        int deliveryInterval = propertiesLoader.getDeliveryInterval();

        for (int i = 0; i < simulationLength; i += tickLength) {
            if (rng.nextDouble() < orderProbability) {
                Point location = new Point(rng.nextDouble() * mapSize, rng.nextDouble() * mapSize);
                int randomStore = rng.nextInt(storeLocations.size());
                int capacity = minCapacity + rng.nextInt(maxCapacity - minCapacity);
                int deliveryStartTime = i + 120000;
                int deliveryEndTime = generateDeliveryEndTime(rng, deliveryStartTime, deliveryInterval);

                // if a scenario only contains the lightweight drones
                if (capacity > capacityDroneLW && amountDroneHW == 0)
                    generateDifferentParcels(scenarioBuilder, i, deliveryStartTime,
                        deliveryEndTime, capacity, randomStore, location);
                if (amountDroneHW != 0) {
                    ParcelDTO orderData = Parcel.builder(storeLocations.get(randomStore), location)
                        .serviceDuration(serviceDuration)
                        .neededCapacity(capacity) // Capacity is measured in grams
                        .deliveryDuration(5)
                        .pickupDuration(5)
                        .orderAnnounceTime(i)
                        .pickupTimeWindow(TimeWindow.create(i, i + 150000))
                        .deliveryTimeWindow(TimeWindow.create(deliveryStartTime, deliveryEndTime))
                        .buildDTO();
                    scenarioBuilder.addEvent(AddOrderEvent.create(orderData));
                }
            }
        }
        for (Point location : storeLocations) {
            scenarioBuilder.addEvent(AddDepotEvent.create(-1, location));
        }

        Point chargingPointLocation = new Point(mapSize / 2, mapSize / 2);

        for (int i = 0; i < amountDroneLW; i++) {
            scenarioBuilder.addEvent(AddDroneEvent.create(new DroneLW(propertiesLoader.getSpeedRangeLW(),
                    capacityDroneLW,
                    propertiesLoader.getBatteryLW(),
                    chargingPointLocation)));
        }
        for (int i = 0; i < amountDroneHW; i++) {
            scenarioBuilder.addEvent(AddDroneEvent.create(new DroneHW(propertiesLoader.getSpeedRangeHW(),
                    capacityDroneHW,
                    propertiesLoader.getBatteryHW(),
                    chargingPointLocation)));
        }
        scenarioBuilder.addEvent(AddChargingPointEvent.create(chargingPointLocation, amountChargersLW, amountChargersHW));

        // Addition of models
        DefaultEnergyModel.Builder energyModel = DefaultEnergyModel.builder();
        scenarioBuilder
            .addModel(RoadModelBuilders.plane()
                .withMinPoint(new Point(0,0))
                .withMaxPoint(new Point(mapSize,mapSize))
                .withDistanceUnit(SI.METER)
                .withSpeedUnit(SI.METERS_PER_SECOND)
                .withMaxSpeed(50))
            // TODO figure out what the different time windows are
            .addModel(DefaultPDPModel.builder().withTimeWindowPolicy(TimeWindowPolicy.TimeWindowPolicies.LIBERAL))
            .addModel(energyModel);

        // Time and timeouts of scenario
        scenarioBuilder
            .scenarioLength(simulationLength)
            .addEvent(TimeOutEvent.create(simulationLength))
            .setStopCondition(StatsStopConditions.timeOutEvent());
        return scenarioBuilder.build();
    }

    private static void generateDifferentParcels(Scenario.Builder scenarioBuilder,
                                                 int time,
                                                 int deliveryStartTime,
                                                 int deliveryEndTime,
                                                 int capacity,
                                                 int randomStore,
                                                 Point location){
        int capacityDroneLW = propertiesLoader.getCapacityLW();
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
            .pickupTimeWindow(TimeWindow.create(time, time + 150000))
            .deliveryTimeWindow(TimeWindow.create(deliveryStartTime, deliveryEndTime))
            .buildDTO();
        scenarioBuilder.addEvent(AddOrderEvent.create(order1));

        // Second part of the order
        ParcelDTO order2 = Parcel.builder(storeLocations.get(randomStore), location)
            .serviceDuration(serviceDuration)
            .neededCapacity(orderWeight2) // Capacity is measured in grams
            .deliveryDuration(5)
            .pickupDuration(5)
            .orderAnnounceTime(time)
            .pickupTimeWindow(TimeWindow.create(time, time + 150000))
            .deliveryTimeWindow(TimeWindow.create(deliveryStartTime, deliveryEndTime))
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
                .pickupTimeWindow(TimeWindow.create(time, time + 150000))
                .deliveryTimeWindow(TimeWindow.create(deliveryStartTime, deliveryEndTime))
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
            .with(MapRenderer.builder(propertiesLoader.getMapLocation()))
            .with(TimeLinePanel.builder())
            .with(StatsPanel.builder())
            .withResolution(new Double(resolutionImage.x).intValue(), new Double(resolutionImage.y).intValue())
            .withTitleAppendix(name)
            .withAutoPlay()
            .withAutoClose();

        if (testing) {
            view = view.withAutoClose()
                .withAutoPlay()
                .withSimulatorEndTime(10000)
                .withSpeedUp(64);
        }
        return view;
    }

    private static int generateDeliveryEndTime(RandomGenerator rng, int startTime, int deliveryInterval){
        int interval = 240000 + rng.nextInt(deliveryInterval);
        return startTime + interval;
    }
}
