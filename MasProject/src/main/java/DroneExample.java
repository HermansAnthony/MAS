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
import util.PropertiesLoader;
import util.Utilities;

import javax.measure.unit.SI;
import java.util.ArrayList;
import java.util.List;

import static util.Utilities.generateDeliveryEndTime;
import static util.Utilities.loadResolutionImage;


public class DroneExample {

    private static List<Point> storeLocations;
    private static Point resolutionImage;
    private static final Display display = new Display();

    private static PropertiesLoader propertiesLoader;

    /**
     * Starts the example.
     *
     * @param testing   Indicates whether the method should run in testing mode.
     * @return The simulator instance.
     */
    public static Simulator run(boolean testing) {
        propertiesLoader = PropertiesLoader.getInstance();
        resolutionImage = loadResolutionImage(propertiesLoader.getMapLocation());
        storeLocations = Utilities.loadStoreLocations(propertiesLoader.getStoresLocation());

        int maxDimension = propertiesLoader.getMapSize();

        final View.Builder view = createGui(testing);
        final Simulator simulator = Simulator.builder()
            .addModel(TimeModel.builder().withTickLength(propertiesLoader.getTickLength()))
            .addModel(RoadModelBuilders.plane()
                .withMinPoint(new Point(0,0))
                .withMaxPoint(new Point(maxDimension,maxDimension))
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

        for (int i = 0; i < Integer.valueOf(propertiesLoader.getProperty("Example.amountLWChargers")); i++) {
            LWChargers.add(new Charger());
            simulator.register(LWChargers.get(i));
        }

        for (int i = 0; i < Integer.valueOf(propertiesLoader.getProperty("Example.amountHWChargers")); i++) {
            HWChargers.add(new Charger());
            simulator.register(HWChargers.get(i));
        }

        Point chargingPointLocation = new Point(maxDimension / 2, maxDimension / 2);
        simulator.register(new ChargingPoint(chargingPointLocation, LWChargers, HWChargers));

        for (Point storeLocation : storeLocations) {
            simulator.register(new Store(storeLocation));
        }
        for (int i = 0; i < Integer.valueOf(propertiesLoader.getProperty("Example.amountLWDrones")); i++) {
            simulator.register(new DroneLW(propertiesLoader.getSpeedRangeLW(),
                propertiesLoader.getCapacityLW(),
                propertiesLoader.getBatteryLW(),
                chargingPointLocation));
        }
        for (int i = 0; i < Integer.valueOf(propertiesLoader.getProperty("Example.amountHWDrones")); i++) {
            simulator.register(new DroneHW(propertiesLoader.getSpeedRangeHW(),
                propertiesLoader.getCapacityHW(),
                propertiesLoader.getBatteryHW(),
                chargingPointLocation));
        }
        panel.initializePanel();

        double orderProbability = Double.valueOf(propertiesLoader.getProperty("Example.orderProbability"));
        int serviceDuration = propertiesLoader.getServiceDuration();
        int minCapacity = Integer.valueOf(propertiesLoader.getProperty("Example.minCapacityOrder"));
        int maxCapacity = Integer.valueOf(propertiesLoader.getProperty("Example.maxCapacityOrder"));
        int deliveryTimeVariance = propertiesLoader.getDeliveryTimeVariance();
        int fixedDeliveryTime = propertiesLoader.getLowerBoundDeliveryTime();

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
                        .neededCapacity(minCapacity + rng.nextInt(maxCapacity - minCapacity))
                        .orderAnnounceTime(time.getStartTime())
                        .pickupTimeWindow(TimeWindow.create(time.getStartTime(), Long.MAX_VALUE))
                        .deliveryTimeWindow(TimeWindow.create(time.getStartTime(), generateDeliveryEndTime(rng, time.getStartTime(), fixedDeliveryTime, deliveryTimeVariance)))
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
            .with(MapRenderer.builder(propertiesLoader.getMapLocation()))
            .withDisplay(display)
            .withResolution(new Double(resolutionImage.x).intValue(), new Double(resolutionImage.y).intValue())
            .withTitleAppendix("Drone Example");
        if (testing) {
            view = view.withAutoClose()
                .withAutoPlay()
                .withSimulatorEndTime(10000)
                .withSpeedUp(64);
        }

        return view;
    }


}
