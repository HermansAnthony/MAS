package ant;

public abstract class Ant {

    AntUser primaryAgent;
    AntUser secondaryAgent;

    Ant(AntUser primaryAgent) {
        this.primaryAgent = primaryAgent;
        this.secondaryAgent = null;
    }


    public void setSecondaryAgent(AntUser secondaryAgent) {
        this.secondaryAgent = secondaryAgent;
    }

    public AntUser getSecondaryAgent() {
        return secondaryAgent;
    }

    public abstract void returnToPrimaryAgent();

    public AntUser getPrimaryAgent() {
        return primaryAgent;
    }
}
