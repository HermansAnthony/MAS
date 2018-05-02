import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DroneDTO extends VehicleDTO {
    DroneDTO() {
    }

    @Override
    public Point getStartPosition() {
        return null;
    }

    @Override
    public double getSpeed() {
        return 0;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public TimeWindow getAvailabilityTimeWindow() {
        return null;
    }

    /**
     * @return A new builder for constructing {@link VehicleDTO}s.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for constructing {@link VehicleDTO}s.
     * @author Rinde van Lon
     */
    public static class Builder {
        private static final double DEFAULT_SPEED = 50d;
        private static final Point DEFAULT_START_POSITION = new Point(0, 0);

        Point startPosition;
        double speed;
        int capacity;
        TimeWindow availabilityTimeWindow;

        Builder() {
            startPosition = DEFAULT_START_POSITION;
            speed = DEFAULT_SPEED;
            capacity = 1;
            availabilityTimeWindow = TimeWindow.always();
        }

        /**
         * Copy the value of the specified vehicle into this builder.
         * @param dto The dto to copy values from.
         * @return This, as per the builder pattern.
         */
        public Builder use(VehicleDTO dto) {
            return startPosition(dto.getStartPosition())
                    .availabilityTimeWindow(dto.getAvailabilityTimeWindow())
                    .speed(dto.getSpeed())
                    .capacity(dto.getCapacity());
        }

        /**
         * Sets the start position of the vehicle. Default value: (0,0).
         * @param point The position.
         * @return This, as per the builder pattern.
         */
        public Builder startPosition(Point point) {
            startPosition = point;
            return this;
        }

        /**
         * Sets the speed of the vehicle. Default value: 50 (using the speed unit of
         * the scenario/simulator where it is used).
         * @param s The speed, must be <code> &gt; 0</code>.
         * @return This, as per the builder pattern.
         */
        public Builder speed(double s) {
            checkArgument(s > 0, "Speed must be positive, found %s.", s);
            speed = s;
            return this;
        }

        /**
         * Sets the capacity of the vehicle. Default value: 1.
         * @param c The capacity, must be <code> &gt;= 0</code>.
         * @return This, as per the builder pattern.
         */
        public Builder capacity(int c) {
            checkArgument(c >= 0, "Capacity may not be negative, found %s.", c);
            capacity = c;
            return this;
        }

        /**
         * Sets the availability {@link TimeWindow} of the vehicle. Default value:
         * {@link TimeWindow#always()}.
         * @param tw The time window.
         * @return This, as per the builder pattern.
         */
        public Builder availabilityTimeWindow(TimeWindow tw) {
            availabilityTimeWindow = tw;
            return this;
        }

        /**
         * @return A new {@link VehicleDTO} instance.
         */
        public VehicleDTO build() {
            return new AutoValue_VehicleDTO(startPosition, speed, capacity,
                    availabilityTimeWindow);
        }
    }
}
