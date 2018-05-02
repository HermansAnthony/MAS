package pdp;

import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

import static com.google.common.base.Preconditions.checkArgument;

public class DroneDTO {
    private static final double DEFAULT_SPEED = 50d;
    private static final Point DEFAULT_START_POSITION = new Point(0, 0);
    private static final int DEFAULT_BATTERY_LIFE = 1000;

    Point startPosition;
    double speed;
    int capacity;
    TimeWindow availabilityTimeWindow;
    int batteryLife;

    DroneDTO(Point _start, double _speed, int _capacity, TimeWindow _window, int _batteryLife) {
        startPosition = _start;
        speed = _speed;
        capacity = _capacity;
        availabilityTimeWindow = _window;
        batteryLife = _batteryLife;
    }


    public void setSpeed(double _speed) {
        speed = _speed;
    }

    public void decreaseBatteryLife(int amt) {
        batteryLife -= amt;
    }

    public int getBatteryLife() {
        return batteryLife;
    }

    public Point getStartPosition() {
        return startPosition;
    }

    public double getSpeed() {
        return speed;
    }

    public int getCapacity() {
        return capacity;
    }

    public TimeWindow getAvailabilityTimeWindow() {
        return availabilityTimeWindow;
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
        Point startPosition;
        double speed;
        int capacity;
        TimeWindow availabilityTimeWindow;
        int batteryLife;

        Builder() {
            startPosition = DEFAULT_START_POSITION;
            speed = DEFAULT_SPEED;
            capacity = 1;
            availabilityTimeWindow = TimeWindow.always();
            batteryLife = DEFAULT_BATTERY_LIFE;
        }

        /**
         * Copy the value of the specified vehicle into this builder.
         * @param dto The dto to copy values from.
         * @return This, as per the builder pattern.
         */
        public Builder use(DroneDTO dto) {
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

        public Builder batteryLife(int _batteryLife) {
            batteryLife = _batteryLife;
            return this;
        }
        /**
         * @return A new {@link DroneDTO} instance.
         */
        public DroneDTO build() {
            return new DroneDTO(startPosition, speed, capacity,
                    availabilityTimeWindow, batteryLife);
        }
    }
}
