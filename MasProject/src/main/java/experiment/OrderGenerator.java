package experiment;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.*;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;

public class OrderGenerator implements Parcels.ParcelGenerator {
    private final RandomGenerator rng;
    private final TimeSeries.TimeSeriesGenerator announceTimeGenerator;
    private final Locations.LocationGenerator locationGenerator;
    private final TimeWindows.TimeWindowGenerator timeWindowGenerator;
    private final StochasticSupplier<Long> pickupDurationGenerator;
    private final StochasticSupplier<Long> deliveryDurationGenerator;
    private final StochasticSupplier<Integer> neededCapacityGenerator;

    private OrderGenerator(Builder b) {
        rng = new MersenneTwister();
        announceTimeGenerator = b.announceTimeGenerator;
        locationGenerator = b.locationGenerator;
        timeWindowGenerator = b.timeWindowGenerator;
        pickupDurationGenerator = b.pickupDurationGenerator;
        deliveryDurationGenerator = b.deliveryDurationGenerator;
        neededCapacityGenerator = b.neededCapacityGenerator;
    }

    @Override
    public ImmutableList<AddParcelEvent> generate(long seed, ScenarioGenerator.TravelTimes travelModel, long endTime) {
        rng.setSeed(seed);
        final ImmutableList.Builder<AddParcelEvent> eventList = ImmutableList
            .builder();
        final List<Double> times = announceTimeGenerator.generate(rng.nextLong());
        final Iterator<Point> locs = locationGenerator.generate(rng.nextLong(),
            times.size() * 2).iterator();

        for (final double time : times) {
            final long arrivalTime = DoubleMath.roundToLong(time,
                RoundingMode.FLOOR);
            final Point origin = locs.next();
            final Point destination = locs.next();

            assert(time < endTime);

            final Parcel.Builder parcelBuilder = Parcel
                .builder(origin, destination)
                .orderAnnounceTime(arrivalTime)
                .pickupDuration(pickupDurationGenerator.get(rng.nextLong()))
                .deliveryDuration(deliveryDurationGenerator.get(rng.nextLong()))
                .neededCapacity(neededCapacityGenerator.get(rng.nextLong()));

            timeWindowGenerator.generate(rng.nextLong(), parcelBuilder,
                travelModel, endTime);

            eventList.add(AddParcelEvent.create(parcelBuilder.buildDTO()));
        }
        return eventList.build();
    }

    @Override
    public Point getCenter() {
        return locationGenerator.getCenter();
    }

    @Override
    public Point getMin() {
        return locationGenerator.getMin();
    }

    @Override
    public Point getMax() {
        return locationGenerator.getMax();
    }

    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        static final TimeSeries.TimeSeriesGenerator DEFAULT_ANNOUNCE_TIMES = TimeSeries
            .homogenousPoisson(4 * 60 * 60 * 1000, 20);
        static final double DEFAULT_AREA_SIZE = 5d;
        static final Locations.LocationGenerator DEFAULT_LOCATIONS = Locations.builder()
            .square(DEFAULT_AREA_SIZE).buildUniform();
        static final TimeWindows.TimeWindowGenerator DEFAULT_TIME_WINDOW_GENERATOR = TimeWindows
            .builder().build();
        static final StochasticSupplier<Long> DEFAULT_SERVICE_DURATION =
            StochasticSuppliers
                .constant(5 * 60 * 1000L);
        static final StochasticSupplier<Integer> DEFAULT_CAPACITY =
            StochasticSuppliers
                .constant(0);

        TimeSeries.TimeSeriesGenerator announceTimeGenerator;
        TimeWindows.TimeWindowGenerator timeWindowGenerator;
        Locations.LocationGenerator locationGenerator;
        StochasticSupplier<Long> pickupDurationGenerator;
        StochasticSupplier<Long> deliveryDurationGenerator;
        StochasticSupplier<Integer> neededCapacityGenerator;


        Builder() {
            announceTimeGenerator = DEFAULT_ANNOUNCE_TIMES;
            timeWindowGenerator = DEFAULT_TIME_WINDOW_GENERATOR;
            locationGenerator = DEFAULT_LOCATIONS;
            pickupDurationGenerator = DEFAULT_SERVICE_DURATION;
            deliveryDurationGenerator = DEFAULT_SERVICE_DURATION;
            neededCapacityGenerator = DEFAULT_CAPACITY;
        }

        /**
         * Sets a {@link TimeSeries.TimeSeriesGenerator} which will be used for generating
         * parcel announce times. The {@link TimeSeries.TimeSeriesGenerator#generate(long)}
         * method returns real-valued times, these are converted to longs by
         * rounding them down using the {@link RoundingMode#FLOOR} strategy.
         * @param atg The time series generator to use.
         * @return This, as per the builder pattern.
         */
        public Builder announceTimes(TimeSeries.TimeSeriesGenerator atg) {
            announceTimeGenerator = atg;
            return this;
        }

        /**
         * Sets a {@link TimeWindows.TimeWindowGenerator} to use for generating parcel pickup
         * and delivery time windows.
         * @param twg The time window generator to use.
         * @return This, as per the builder pattern.
         */
        public Builder timeWindows(TimeWindows.TimeWindowGenerator twg) {
            timeWindowGenerator = twg;
            return this;
        }

        /**
         * Sets a {@link Locations.LocationGenerator} to use for generating parcel pickup and
         * delivery locations.
         * @param lg The location generator to use.
         * @return This, as per the builder pattern.
         */
        public Builder locations(Locations.LocationGenerator lg) {
            locationGenerator = lg;
            return this;
        }

        /**
         * Sets the durations of the parcel pickup operations.
         * @param durations The supplier to draw the durations from.
         * @return This, as per the builder pattern.
         */
        public Builder pickupDurations(StochasticSupplier<Long> durations) {
            pickupDurationGenerator = durations;
            return this;
        }

        /**
         * Sets the durations of the parcel delivery operations.
         * @param durations The supplier to draw the durations from.
         * @return This, as per the builder pattern.
         */
        public Builder deliveryDurations(StochasticSupplier<Long> durations) {
            deliveryDurationGenerator = durations;
            return this;
        }

        /**
         * Sets the durations of the parcel pickup and delivery operations.
         * @param durations The supplier to draw the durations from.
         * @return This, as per the builder pattern.
         */
        public Builder serviceDurations(StochasticSupplier<Long> durations) {
            return pickupDurations(durations).deliveryDurations(durations);
        }

        /**
         * Sets the capacities that are needed to carry the generated parcels.
         * @param capacities The supplier to draw the capacities from.
         * @return This, as per the builder pattern.
         */
        public Builder neededCapacities(StochasticSupplier<Integer> capacities) {
            neededCapacityGenerator = capacities;
            return this;
        }


        /**
         * @return A new {@link Parcels.ParcelGenerator} instance.
         */
        public Parcels.ParcelGenerator build() {
            return new OrderGenerator(this);
        }
    }
}
