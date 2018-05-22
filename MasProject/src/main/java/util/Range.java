package util;

public class Range {
    private int low;
    private int high;

    public Range(int low, int high) {
        assert(low <= high);
        this.low = low;
        this.high = high;
    }

    /**
     * Return the speed in range, according to the percentage
     * @param percentage the percentage of the maximum speed
     * @return the speed;
     */
    public int getSpeed(double percentage) {
        Long additionalValue = Math.round((high - low) * percentage);
        return low + additionalValue.intValue();
    }
}
