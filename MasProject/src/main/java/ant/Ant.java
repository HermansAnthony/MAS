package ant;

public abstract class Ant {

    private AntReceiver primaryAgent;
    private AntReceiver secondaryAgent; // TODO advanced exploration ants going to multiple nodes -> sort of planning

    Ant(AntReceiver primaryAgent) {
        this.primaryAgent = primaryAgent;
        this.secondaryAgent = null;
    }


    public void setSecondaryAgent(AntReceiver secondaryAgent) {
        this.secondaryAgent = secondaryAgent;
    }

    public AntReceiver getSecondaryAgent() {
        return secondaryAgent;
    }

    public void returnToPrimaryAgent() {
        primaryAgent.receiveAnt(this);
    }

    public AntReceiver getPrimaryAgent() {
        return primaryAgent;
    }
}
