package ant;

public abstract class Ant {

    AntUser primaryAgent;
    AntUser secondaryAgent; // TODO advanced exploration ants going to multiple nodes -> sort of planning

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
