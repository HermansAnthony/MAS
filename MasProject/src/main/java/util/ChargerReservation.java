package util;

import pdp.Drone;

public class ChargerReservation {
    private Drone owner;
    private long timeBegin;
    private long timeEnd;

    public ChargerReservation(Drone owner, long timeBegin, long timeEnd) {
        this.owner = owner;
        this.timeBegin = timeBegin;
        this.timeEnd = timeEnd;
    }

    public Drone getOwner() {
        return owner;
    }

    public Tuple<Long, Long> getTimeWindow() {
        return new Tuple<>(timeBegin, timeEnd);
    }

    /**
     * Checks if the given time frame conflicts with this reservation.
     * @param timeBegin The begin of the time frame.
     * @param timeEnd The end of the time frame.
     * @return True if the time windows are conflicting.
     */
    public boolean conflicts(long timeBegin, long timeEnd) {
        return (this.timeBegin <= timeBegin && timeBegin <= this.timeEnd)
            || (this.timeBegin <= timeEnd && timeEnd <= this.timeEnd)
            || (timeBegin <= this.timeBegin && this.timeEnd <= timeEnd);
    }
}