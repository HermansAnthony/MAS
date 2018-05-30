package ant;

import pdp.Drone;

public class ChargeIntentionAnt extends IntentionAnt {
    private long beginTime;
    private long endTime;

    public ChargeIntentionAnt(Drone primaryAgent, AntUser destination, long beginTime, long endTime) {
        super(primaryAgent, destination);

        this.beginTime = beginTime;
        this.endTime = endTime;
    }


    public long getBeginTime() {
        return beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
