import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.energy.ChargingPoint;
import com.github.rinde.rinsim.core.model.energy.DefaultEnergyModel;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;
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


public class DroneExample {

    private static final String map = "target/classes/leuven.png";

    private static final int endTime = 60000;

    // LW = light weight, HW = heavy weight
    private static final int amountDroneLW = 20;
    private static final int amountDroneHW = 10;

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

    private static List<Point> storeLocations;
    private static Point resolution;


    /**
     * Starts the {@link DroneExample}.
     *
     * @param args The first option may optionally indicate the end time of the
     *             simulation.
     */
    public static void main(@Nullable String[] args) {
        loadResolutionImage(map);
        loadStoreLocations("src/main/resources/stores.csv");

        run(false, endTime);
    }


    /**
     * Run the example.
     *
     * @param testing If <code>true</code> enables the test mode.
     */
    public static void run(boolean testing) {
        run(testing, Long.MAX_VALUE);
    }

    /**
     * Starts the example.
     *
     * @param testing   Indicates whether the method should run in testing mode.
     * @param endTime   The time at which simulation should stop.
     * @return The simulator instance.
     */
    private static Simulator run(boolean testing, final long endTime) {
        final View.Builder view = createGui(testing);
        // use map of leuven
        final Simulator simulator = Simulator.builder()
            .addModel(TimeModel.builder().withTickLength(250))
            .addModel(RoadModelBuilders.plane()
//                .withObjectRadius(droneRadius)
                .withMinPoint(new Point(0,0))
                .withMaxPoint(new Point(5000,5000))
                .withDistanceUnit(SI.METER)
                .withSpeedUnit(SI.METERS_PER_SECOND)
                .withMaxSpeed(50))
            .addModel(DefaultEnergyModel.builder())
            .addModel(DefaultPDPModel.builder()) // TODO possibly define our own PDP model, extended from the PDP model class
            .addModel(view)
            .build();
        final RandomGenerator rng = simulator.getRandomGenerator();
        final PlaneRoadModel planeRoadModel = simulator.getModelProvider().getModel(PlaneRoadModel.class);


        simulator.register(new ChargingPoint(chargingPointLocation, amountChargersLW, amountChargersHW));

        for (Point storeLocation : storeLocations) {
            simulator.register(new Store(storeLocation));
        }
        for (int i = 0; i < amountDroneLW; i++) {
            simulator.register(new DroneLW(speedDroneLW, capacityDroneLW, batteryDroneLW, chargingPointLocation));
        }
        for (int i = 0; i < amountDroneHW; i++) {
            simulator.register(new DroneHW(speedDroneHW, capacityDroneHW, batteryDroneHW, chargingPointLocation));
        }

        simulator.addTickListener(new TickListener() {
            @Override
            public void tick(@NotNull TimeLapse time) {
                if (rng.nextDouble() < orderProbability) {
                    Point location = planeRoadModel.getRandomPosition(rng);
                    Customer customer = new Customer(location);
                    simulator.register(customer);
                    int randomStore = rng.nextInt(storeLocations.size());
                    ParcelDTO orderData = Parcel.builder(storeLocations.get(randomStore),location)
                            .serviceDuration(serviceDuration)
                            .neededCapacity(1000 + rng.nextInt(maxCapacity - 1000)) // Capacity is measured in grams
                            .deliveryDuration(5)
                            .pickupDuration(5)
                            .buildDTO();
                    simulator.register(new Order(orderData, customer));
                }
            }

            @Override
            public void afterTick(@NotNull TimeLapse timeLapse) {}
        });

        simulator.start();
        return simulator;
    }

    private static View.Builder createGui(boolean testing) {
        View.Builder view = View.builder()
            .with(PlaneRoadModelRenderer.builder())
            .with(RoadUserRenderer.builder()
                .withImageAssociation(
                    Customer.class, "/customer-32.png")
                .withImageAssociation(
                    Store.class, "/store-40.png")
                .withImageAssociation(
                    ChargingPoint.class, "/chargingPoint-40.png")
                .withImageAssociation(
                    DroneLW.class, "/droneLW-32.png")
                .withImageAssociation(
                    DroneHW.class, "/droneHW-32.png"))
            .with(DroneRenderer.builder())
            .with(MapRenderer.builder(map))
            .withResolution(new Double(resolution.x).intValue(), new Double(resolution.y).intValue())
            .withTitleAppendix("Drone Demo - WIP");

        if (testing) {
            view = view.withAutoClose()
                .withAutoPlay()
                .withSimulatorEndTime(10000)
                .withSpeedUp(64);
        }
        return view;
    }


    /**
     * Load the resolution of the given image.
     * @param filename the image file.
     */
    private static void loadResolutionImage(String filename) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(filename));
            resolution = new Point(bufferedImage.getWidth(), bufferedImage.getHeight());
        } catch (IOException e) {
            resolution = new Point(800,600);
        }
    }

    /**
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
