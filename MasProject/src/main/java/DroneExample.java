import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import javax.annotation.Nullable;


public class DroneExample {
    private DroneExample() {
    }

    private static final String map = "/resources/leuven.png";

    private static final int endTime = 60000;

    // LW = light weight, HW = heavy weight
    private static final int amountDroneLW = 15;
    private static final int amountDroneHW = 15;
    private static final int amountChargersLW = 5;
    private static final int amountChargersHW = 5;
    private static final int amountRequests = 100;


    /**
     * Starts the {@link DroneExample}.
     *
     * @param args The first option may optionally indicate the end time of the
     *             simulation.
     */
    public static void main(@Nullable String[] args) {
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

        final View.Builder view = createGui(testing, display, m, list);

        // use map of leuven
        final Simulator simulator = Simulator.builder()
                .addModel(DefaultPDPModel.builder())
                .addModel(view)
                .build();
        final RandomGenerator rng = simulator.getRandomGenerator();
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
//        simulator.addTickListener(new TickListener() {
//            @Override
//            public void tick(TimeLapse time) {
//                if (time.getStartTime() > endTime) {
//                    simulator.stop();
//                } else if (rng.nextDouble() < NEW_CUSTOMER_PROB) {
//                    simulator.register(new TaxiExample.Customer(
//                            Parcel
//                                    .builder(roadModel.getRandomPosition(rng),
//                                            roadModel.getRandomPosition(rng))
//                                    .serviceDuration(SERVICE_DURATION)
//                                    .neededCapacity(1 + rng.nextInt(MAX_CAPACITY))
//                                    .buildDTO()));
//                }
//            }

    static View.Builder createGui(
            boolean testing,
            @Nullable Display display,
            @Nullable Monitor m,
            @Nullable Listener list) {

        View.Builder view = View.builder()
                .with(GraphRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(
                                Store.class, "/resources/store.png")
                        .withImageAssociation(
                                ChargingPoint.class, "/resources/chargingPoint.png")
                        .withImageAssociation(
                                DroneLW.class, "/resources/droneLW.png")
                        .withImageAssociation(
                                DroneHW.class, "/resources/droneHW.png")
                        .withImageAssociation(
                                Customer.class, "/resources/customer.png"))
                .with(DroneRenderer.builder())
                .withTitleAppendix("Drone Demo");

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
