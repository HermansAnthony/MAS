package experiment;

import java.io.Serializable;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import pdp.Customer;
import pdp.Order;
/**
 * Event indicating that a parcel can be created.
 * @author Anthony Hermans, Federico Quin
 */
public class AddOrderEvent implements TimedEvent {
    long time;
    ParcelDTO parcel;
    private Customer customer;

    AddOrderEvent(long _time, ParcelDTO _parcel, Point customerLocation) {
        time = _time;
        parcel = _parcel;
        customer = new Customer(customerLocation);
    }

    /**
     * @return The data which should be used to instantiate a new parcel.
     */
    public ParcelDTO getParcelDTO(){return parcel;}

    /**
     * @return The customer the specific order.
     */
    public Customer getCustomer(){return customer;}

    /**
     * Creates a new {@link AddOrderEvent}.
     * @param dto The {@link ParcelDTO} that describes the parcel.
     * @return A new instance.
     */
    public static AddOrderEvent create(ParcelDTO dto, Point customer) {
        return new AddOrderEvent(dto.getOrderAnnounceTime(), dto, customer);
    }

    /**
     * Default {@link TimedEventHandler} that creates a {@link Parcel} for every
     * {@link AddOrderEvent} that is received.
     * @return The default handler.
     */
    public static TimedEventHandler<AddOrderEvent> defaultHandler() {
        return Handler.INSTANCE;
    }

    /**
     * {@link TimedEventHandler} that creates {@link Parcel}s with an overridden
     * toString implementation. The first 26 parcels are named
     * <code>A,B,C,..,Y,Z</code>, parcel 27 to 702 are named
     * <code>AA,AB,..,YZ,ZZ</code>. If more than 702 parcels are created the
     * {@link TimedEventHandler} will throw an {@link IllegalStateException}.
     * <p>
     * <b>Warning:</b> This handler should only be used for debugging purposes and
     * is not thread safe.
     * @return A newly constructed handler.
     */
    public static TimedEventHandler<AddOrderEvent> namedHandler() {
        return new NamedOrderCreator();
    }

    @Override
    public long getTime() {
        return time;
    }

    enum Handler implements TimedEventHandler<AddOrderEvent> {
        INSTANCE {
            @Override
            public void handleTimedEvent(AddOrderEvent event, SimulatorAPI sim) {
                sim.register(event.getCustomer());
                sim.register(Parcel.builder(event.getParcelDTO()).build());
            }

            @Override
            public String toString() {
                return AddOrderEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };
    }

    static class NamedOrderCreator
            implements TimedEventHandler<AddOrderEvent>, Serializable {
        private static final long serialVersionUID = 3888253170041895475L;

        private static final int ALPHABET_SIZE = 26;
        private static final int PARCEL_LIMIT =
                ALPHABET_SIZE + ALPHABET_SIZE * ALPHABET_SIZE;


        NamedOrderCreator() {}

        @Override
        public void handleTimedEvent(AddOrderEvent event, SimulatorAPI simulator) {
            simulator.register(event.getCustomer());
            simulator.register(new Order(event.getParcelDTO(),event.getCustomer()));
        }

        @Override
        public String toString() {
            return AddOrderEvent.class.getSimpleName() + ".namedHandler()";
        }
    }
}
