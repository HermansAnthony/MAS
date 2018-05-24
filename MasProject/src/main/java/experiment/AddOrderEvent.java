package experiment;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import pdp.Customer;
import pdp.Order;

/**
 * Event indicating that a parcel can be created.
 * @author Anthony Hermans, Federico Quin
 */
public class AddOrderEvent implements TimedEvent {
    private long time;
    private ParcelDTO parcel;

    private AddOrderEvent(long time, ParcelDTO parcel) {
        this.time = time;
        this.parcel = parcel;
    }

    /**
     * @return The data which should be used to instantiate a new parcel.
     */
    private ParcelDTO getParcelDTO(){return parcel;}

    /**
     * Creates a new {@link AddOrderEvent}.
     * @param dto The {@link ParcelDTO} that describes the parcel.
     * @return A new instance.
     */
    public static AddOrderEvent create(ParcelDTO dto) {
        return new AddOrderEvent(dto.getOrderAnnounceTime(), dto);
    }

    /**
     * Default {@link TimedEventHandler} that creates a {@link Parcel} for every
     * {@link AddOrderEvent} that is received.
     * @return The default handler.
     */
    public static TimedEventHandler<AddOrderEvent> defaultHandler() {
        return Handler.INSTANCE;
    }

    @Override
    public long getTime() {
        return time;
    }

    enum Handler implements TimedEventHandler<AddOrderEvent> {
        INSTANCE {
            @Override
            public void handleTimedEvent(AddOrderEvent event, SimulatorAPI sim) {
                Customer customer = new Customer(event.getParcelDTO().getDeliveryLocation());
                sim.register(customer);
                sim.register(new Order(event.getParcelDTO(), customer));
            }

            @Override
            public String toString() {
                return AddOrderEvent.class.getSimpleName() + ".defaultHandler()";
            }
        }
    }
}
