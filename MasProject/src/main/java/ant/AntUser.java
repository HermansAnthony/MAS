package ant;

import com.github.rinde.rinsim.core.model.road.RoadUser;

// TODO maybe rethink inheritance of RoadUser here
public interface AntUser extends RoadUser {
    void receiveExplorationAnt(ExplorationAnt ant);
    void receiveIntentionAnt(IntentionAnt ant);

    String getDescription();
}
