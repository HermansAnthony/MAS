package energy;

import ant.Ant;
import ant.AntReceiver;
import ant.ExplorationAnt;
import com.github.rinde.rinsim.core.model.time.TickListener;
import pdp.Drone;
import pdp.DroneHW;
import pdp.DroneLW;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChargingPoint implements AntReceiver, RoadUser, EnergyUser, TickListener {
    private Point location;
    private Map<Class<?>, List<Drone>> chargers;
    private List<Ant> temporaryAnts;

    public ChargingPoint(Point loc, int maxCapacityLW, int maxCapacityHW) {
        location = loc;
        chargers = new HashMap<>();
        chargers.put(DroneLW.class, Arrays.asList(new Drone[maxCapacityLW]));
        chargers.put(DroneHW.class, Arrays.asList(new Drone[maxCapacityHW]));
        temporaryAnts = new ArrayList<>();
    }

    @Override
    public void initRoadUser(@NotNull RoadModel roadModel) {
        roadModel.addObjectAt(this, location);
    }

    public void chargeDrone(Drone drone) {
        assert(!this.chargersOccupied(drone.getClass()));
        List<Drone> drones = chargers.get(drone.getClass());
        for (int i = 0; i < drones.size(); i++) {
            if (drones.get(i) == null) {
                drones.set(i, drone);
            }
        }
    }

    public boolean chargersOccupied(Drone drone) {
        return chargersOccupied(drone.getClass());
    }

    private boolean chargersOccupied(Class droneClass) {
        return !chargers.get(droneClass).contains(null);
    }

    public double occupationPercentage(Class droneClass) {
        return chargers.get(droneClass).stream().filter(Objects::nonNull).count() / chargers.get(droneClass).size();
    }

    public boolean dronePresent(Drone drone) {
        return chargers.get(drone.getClass()).contains(drone);
    }


    /**
     * Charges all the drones present in the ChargingPoint.
     * TODO This method also keeps track of the occupation of the charging station, since it is called every tick.
     * @param timeLapse timelapse.
     */
    public void charge(TimeLapse timeLapse) {
        double tickLength = timeLapse.getTickLength();

        for (List<Drone> drones : chargers.values()) {
            drones.stream().filter(Objects::nonNull).forEach(o -> o.battery.recharge(tickLength / 1000));
        }
    }

    public List<Drone> redeployChargedDrones() {
        List<Drone> redeployableDrones = new ArrayList<>();

        for (List<Drone> drones : chargers.values()) {
            for (int i = 0; i < drones.size(); i++) {
                if (drones.get(i) != null) {
                    if (drones.get(i).battery.fullyCharged()) {
                        redeployableDrones.add(drones.get(i));
                        drones.set(i, null);
                    }
                }
            }
        }
        return redeployableDrones;
    }

    public String getStatus() {
        String status = "The current occupation of the charging point is: \n";
        status += chargers.get(DroneLW.class).stream().filter(Objects::nonNull).count() + " lightweight drones are charging\n";
        status += chargers.get(DroneHW.class).stream().filter(Objects::nonNull).count() + " heavyweight drones are charging";
        return status;
    }

    @Override
    public void initEnergyUser(EnergyModel energyModel) {
        // TODO Empty for now, if necessary store energyModel here
    }

    public final Point getLocation() {
        return location;
    }

    @Override
    public void receiveAnt(Ant ant) {
        if (ant instanceof ExplorationAnt) {
            ExplorationAnt explorationAnt = (ExplorationAnt) ant;
            explorationAnt.setChargingPointOccupation(this.occupationPercentage(ant.getPrimaryAgent().getClass()));
        }

        synchronized (temporaryAnts) {
            temporaryAnts.add(ant);
        }
    }

    @Override
    public void tick(@NotNull TimeLapse timeLapse) {
        // TODO move charging to here from energymodel
        if (!timeLapse.hasTimeLeft())
            return;

        synchronized (temporaryAnts) {
            for (Ant ant : temporaryAnts) {
                ant.returnToPrimaryAgent();
            }
            temporaryAnts.clear();
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {}
}
