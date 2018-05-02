import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;
import pdp.ContainerImpl;
import pdp.DroneDTO;
import pdp.DronePDPType;

public abstract class Drone extends ContainerImpl implements MovingRoadUser,
        TickListener {

    final DroneDTO dto;
    protected boolean hasOrder;
    protected Optional<Parcel> payload;


    protected Drone(DroneDTO droneDTO) {
        dto = droneDTO;
        setStartPosition(dto.getStartPosition());
        setCapacity(dto.getCapacity());
        hasOrder = false;
        payload = Optional.absent();
    }


    public final DronePDPType getType() {
        return DronePDPType.DRONE;
    }


    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    /**
     * Is called every tick. This replaces the
     * {@link TickListener#tick(TimeLapse)} for vehicles.
     * @param time The time lapse that can be used.
     * @see TickListener#tick(TimeLapse)
     */
    protected abstract void tickImpl(TimeLapse time);

    @Override
    public void afterTick(TimeLapse time) {}

    /**
     * @return The data transfer object which holds the immutable properties of
     *         this vehicle.
     */
    public final DroneDTO getDTO() {
        return dto;
    }

    @Override
    public final double getSpeed() {
        return dto.getSpeed();
    }

    /**
     * @return The time window in which this vehicle is available.
     */
    public final TimeWindow getAvailabilityTimeWindow() {
        return dto.getAvailabilityTimeWindow();
    }

    /**
     * @return The start position of the vehicle.
     */
    public final Point getStartPosition() {
        return dto.getStartPosition();
    }
}
