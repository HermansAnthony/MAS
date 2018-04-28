import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.examples.taxi.TaxiExample;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

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
    private DroneExample() {}

    private static final String map = "target/classes/leuven.png";

    private static final int endTime = 60000;

    // LW = light weight, HW = heavy weight
    private static final int amountDroneLW = 15;
    private static final int amountDroneHW = 15;
    private static final int droneRadius = 1;
    private static final int amountChargersLW = 5;
    private static final int amountChargersHW = 5;
    private static final int amountRequests = 100;
    private static final double orderPropability = 0.005;
    private static final int serviceDuration = 60000;
    private static final int maxCapacity = 9;



    private static List<Point> storeLocations;


    private static Point resolution;


    /**
     * Starts the {@link DroneExample}.
     *
     * @param args The first option may optionally indicate the end time of the
     *             simulation.
     */
    public static void main(@Nullable String[] args) {
        // Get resolution image
        try {
            BufferedImage bimg = ImageIO.read(new File(map));
            resolution = new Point(bimg.getWidth(), bimg.getHeight());
        } catch (IOException e) {
            resolution = new Point(800,600);
        }

        // Read store locations from csv file
        storeLocations = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File("target/classes/stores.csv"));
            scanner.useDelimiter(",|\n");
            while(scanner.hasNext()){
                storeLocations.add(new Point(new Double(scanner.next()), new Double(scanner.next())));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not read csv file with store locations.");
        }

        System.out.println(storeLocations);

        run(false, endTime, map, null, null, null);
    }

    /**
     * Run the example.
     *
     * @param testing If <code>true</code> enables the test mode.
     */
    public static void run(boolean testing) {
        run(testing, Long.MAX_VALUE, map, null, null, null);
    }

    /**
     * Starts the example.
     *
     * @param testing   Indicates whether the method should run in testing mode.
     * @param endTime   The time at which simulation should stop.
     * @param graphFile The graph that should be loaded.
     * @param display   The display that should be used to show the ui on.
     * @param m         The monitor that should be used to show the ui on.
     * @param list      A listener that will receive callbacks from the ui.
     * @return The simulator instance.
     */
    public static Simulator run(boolean testing, final long endTime,
                                String graphFile,
                                @Nullable Display display, @Nullable Monitor m, @Nullable Listener list) {

        System.out.println("Creating the view: INIT\n");
        final View.Builder view = createGui(testing, display, m, list);
        System.out.println("Creating the view: DONE\n");


        // use map of leuven
        System.out.println("Creating the simulator: INIT\n");
        final Simulator simulator = Simulator.builder()
                .addModel(TimeModel.builder().withTickLength(250))
                .addModel(RoadModelBuilders.plane()
//                    .withCollisionAvoidance()
//                    .withObjectRadius(droneRadius)
                    .withMinPoint(new Point(0,0))
                    .withMaxPoint(resolution))
//                    .withDistanceUnit(SI.METER)
//                    .withSpeedUnit(SI.METERS_PER_SECOND)
//                    .withMaxSpeed(1))
//                .addModel(DefaultPDPModel.builder()) // TODO possibly define our own PDP model, extended from the PDP model class, see RinSim/core/src/main/java/com/github/rinde/rinsim/core/model/pdp/PDPModel.java
                .addModel(view)
                .build();
        System.out.println("Creating the simulator: DONE\n");
        final RandomGenerator rng = simulator.getRandomGenerator();
        System.out.println("Starting simulator ...\n");

        final PlaneRoadModel planeRoadModel = simulator.getModelProvider().getModel(
                PlaneRoadModel.class);

        for (int i = 0; i < 1; i++) {
            simulator.register(new DroneLW());
        }
        for (int i = 0; i < 1; i++) {
            simulator.register(new DroneHW());
        }
        for (int i = 0; i < storeLocations.size(); i++) {
            simulator.register(new Store(storeLocations.get(i)));
        }

        simulator.register(new ChargingPoint(new Point(560,478)));


        simulator.addTickListener(new TickListener() {
            @Override
            public void tick(TimeLapse time) {
                if (rng.nextDouble() < orderPropability) {
                    simulator.register(new Customer(
                            Parcel.builder(planeRoadModel.getRandomPosition(rng),
                                            planeRoadModel.getRandomPosition(rng))
                                    .serviceDuration(serviceDuration)
                                    .neededCapacity(1 + rng.nextInt(maxCapacity - 1))
                                    .buildDTO()));
                }
            }

            @Override
            public void afterTick(TimeLapse timeLapse) {}
        });

        simulator.start();


        return simulator;
    }

//        final RoadModel roadModel = simulator.getModelProvider().getModel(
//                RoadModel.class);
//        // add depots, taxis and parcels to simulator
//        for (int i = 0; i < NUM_DEPOTS; i++) {
//            simulator.register(new TaxiExample.TaxiBase(roadModel.getRandomPosition(rng),
//                    DEPOT_CAPACITY));
//        }
//        for (int i = 0; i < NUM_TAXIS; i++) {
//            simulator.register(new Taxi(roadModel.getRandomPosition(rng),
//                    TAXI_CAPACITY));
//        }
//        for (int i = 0; i < NUM_CUSTOMERS; i++) {
//            simulator.register(new TaxiExample.Customer(
//                    Parcel.builder(roadModel.getRandomPosition(rng),
//                            roadModel.getRandomPosition(rng))
//                            .serviceDuration(SERVICE_DURATION)
//                            .neededCapacity(1 + rng.nextInt(MAX_CAPACITY))
//                            .buildDTO()));
//        }
//
//            }

    static View.Builder createGui(
            boolean testing,
            @Nullable Display display,
            @Nullable Monitor m,
            @Nullable Listener list) {


        View.Builder view = View.builder()
                .with(PlaneRoadModelRenderer.builder()) // TODO verify if necessary here
                .with(RoadUserRenderer.builder()
                    .withImageAssociation(
                        DroneLW.class, "/droneLW-32.png")
                    .withImageAssociation(
                        DroneHW.class, "/droneHW-32.png")
                    .withImageAssociation(
                        Customer.class, "/customer-32.png")
                    .withImageAssociation(
                        Store.class, "/store-40.png")
                    .withImageAssociation(
                        ChargingPoint.class, "/chargingPoint-40.png"))
                .with(DroneRenderer.builder())
                .with(MapRenderer.builder(map))
                .withResolution(new Double(resolution.x).intValue(), new Double(resolution.y).intValue())
                .withTitleAppendix("Drone Demo - WIP");

        if (testing) {
            view = view.withAutoClose()
                    .withAutoPlay()
                    .withSimulatorEndTime(10000)
                    .withSpeedUp(64);
        } else if (m != null && list != null && display != null) {
            view = view.withMonitor(m)
                    .withSpeedUp(64)
                    .withResolution(m.getClientArea().width, m.getClientArea().height)
                    .withDisplay(display)
                    .withCallback(list)
                    .withAsync()
                    .withAutoPlay()
                    .withAutoClose();
        }
        return view;
    }
}
