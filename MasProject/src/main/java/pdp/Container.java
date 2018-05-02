package pdp;


/**
 * TODO ranzige copy paste
 */
public interface Container extends PDPObject {

    /**
     * The returned value is treated as a constant (i.e. it is read only once).
     * @return The maximum capacity of the container.
     */
    double getCapacity();
}