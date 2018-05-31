package ant;

public interface AntUser {
    void receiveExplorationAnt(ExplorationAnt ant);
    void receiveIntentionAnt(IntentionAnt ant);

    String getDescription();
}
