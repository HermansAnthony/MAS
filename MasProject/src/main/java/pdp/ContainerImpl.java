package pdp;

import static com.google.common.base.Preconditions.checkState;

/**
 * TODO ranzige copy paste
 */
public abstract class ContainerImpl extends PDPObjectImpl implements Container {

    private double capacity;

    /**
     * Sets the capacity. This must be set before the object is registered in its
     * model.
     * @param pCapacity The capacity to use.
     */
    protected final void setCapacity(double pCapacity) {
        checkState(!isRegistered(),
                "capacity must be set before object is registered, it can not be "
                        + "changed afterwards.");
        capacity = pCapacity;
    }

    @Override
    public final double getCapacity() {
        return capacity;
    }
}
