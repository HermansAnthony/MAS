import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.TimeWindow;
import energy.Charger;
import energy.ChargingPoint;
import energy.DefaultEnergyModel;
import energy.EnergyModel;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.jetbrains.annotations.NotNull;
import pdp.*;
import renderer.ChargePanel;
import renderer.DroneRenderer;
import renderer.MapRenderer;
import util.Range;
import util.Utilities;

import javax.imageio.ImageIO;
import javax.measure.unit.SI;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



public class DroneExample {

    private static final String map = "/leuven2_800x800.png";

    private static final int endTime = 60000;

    // LW = light weight, HW = heavy weight
    private static final int amountDroneLW = 20;
    private static final int amountDroneHW = 10;
//    private static final int amountDroneLW = 0;
//    private static final int amountDroneHW = 1;

    private static final Range speedDroneLW = new Range(17,22);
    private static final Range speedDroneHW = new Range(11,22);

    private static int capacityDroneLW = 3500; // Expressed in grams
    private static int capacityDroneHW = 9000; // Expressed in grams

    private static int batteryDroneLW = 2400;  // Expressed in seconds
    private static int batteryDroneHW = 1500;  // Expressed in seconds

    private static final int amountChargersLW = 5;
    private static final int amountChargersHW = 5;
    private static final double orderProbability = 0.005;
    private static final int serviceDuration = 60000;
    private static final int maxCapacity = 9000;

    private static final Point chargingPointLocation = new Point(2500,2500);

    private static List<Point> storeLocations;
    private static Point resolutionImage;
    private static final Display display = new Display();


    /**
     * Starts the {@link DroneExample}.
     *
     * @param args The first option may optionally indicate the end time of the
     *             simulation.
     */
//    public static void main(@Nullable String[] args) {
//        run(false, endTime);
//    }


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
        loadResolutionImage(map);
        storeLocations = Utilities.loadStoreLocations("/stores.csv");

        final View.Builder view = createGui(testing);
        final Simulator simulator = Simulator.builder()
            .addModel(TimeModel.builder().withTickLength(250))
            .addModel(RoadModelBuilders.plane()
                .withMinPoint(new Point(0,0))
                .withMaxPoint(new Point(5000,5000))
                .withDistanceUnit(SI.METER)
                .withSpeedUnit(SI.METERS_PER_SECOND)
                .withMaxSpeed(50))
            .addModel(DefaultEnergyModel.builder())
            .addModel(DefaultPDPModel.builder())
            .addModel(view)
            .build();
        final RandomGenerator rng = simulator.getRandomGenerator();
        final PlaneRoadModel planeRoadModel = simulator.getModelProvider().getModel(PlaneRoadModel.class);
        final EnergyModel energyModel = simulator.getModelProvider().getModel(EnergyModel.class);
        final ChargePanel panel = new ChargePanel(display, energyModel);

        List<Charger> LWChargers = new ArrayList<>();
        List<Charger> HWChargers = new ArrayList<>();

        for (int i = 0; i < amountChargersLW; i++) {
            LWChargers.add(new Charger());
            simulator.register(LWChargers.get(i));
        }

        for (int i = 0; i < amountChargersHW; i++) {
            HWChargers.add(new Charger());
            simulator.register(HWChargers.get(i));
        }

        simulator.register(new ChargingPoint(chargingPointLocation, LWChargers, HWChargers));

        for (Point storeLocation : storeLocations) {
            simulator.register(new Store(storeLocation));
        }
        for (int i = 0; i < amountDroneLW; i++) {
            simulator.register(new DroneLW(speedDroneLW, capacityDroneLW, batteryDroneLW, chargingPointLocation));
        }
        for (int i = 0; i < amountDroneHW; i++) {
            simulator.register(new DroneHW(speedDroneHW, capacityDroneHW, batteryDroneHW, chargingPointLocation));
        }
        panel.initializePanel();
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
                        .orderAnnounceTime(time.getStartTime())
                        .neededCapacity(1000 + rng.nextInt(maxCapacity - 1000)) // Capacity is measured in grams
                        .deliveryDuration(5)
                        .pickupTimeWindow(TimeWindow.create(time.getEndTime(), time.getEndTime()+10000))
                        .pickupDuration(5)
                        .buildDTO();
                    simulator.register(new Order(orderData, customer));
                }
            }

            @Override
            public void afterTick(@NotNull TimeLapse timeLapse) {}
        });

        panel.render();
        simulator.start();
        if (!display.isDisposed()) display.dispose();
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
            .withDisplay(display)
            .withResolution(new Double(resolutionImage.x).intValue(), new Double(resolutionImage.y).intValue())
            .withTitleAppendix("Drone Demo");
        if (testing) {
            view = view.withAutoClose()
                .withAutoPlay()
                .withSimulatorEndTime(10000)
                .withSpeedUp(64);
        }

        return view;
    }


    /**
     * Load the resolutionImage of the given image.
     * @param filename the image file.
     */
    private static void loadResolutionImage(String filename) {
        try {
            BufferedImage bufferedImage = ImageIO.read(DroneExample.class.getResource(filename));
            resolutionImage = new Point(bufferedImage.getWidth(), bufferedImage.getHeight());
        } catch (IOException e) {
            resolutionImage = new Point(1120.0,956.0);
        }
    }

}
