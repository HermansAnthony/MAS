import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.base.Optional;

public abstract class Drone extends Vehicle {
    protected boolean hasOrder;
    protected Optional<Parcel> payload;


    protected Drone(VehicleDTO vehicleDto) {
        super(vehicleDto);
        hasOrder = false;
        payload = Optional.absent();
    }
}
