/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.DeadlockException;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

class AGVAgent implements TickListener, MovingRoadUser {
  private final RandomGenerator rng;
  private Optional<CollisionGraphRoadModelImpl> roadModel;
  private Optional<Point> destination;
  private LinkedList<Point> path;
  private int index;

  AGVAgent(RandomGenerator r) {
    rng = r;
    roadModel = Optional.absent();
    destination = Optional.absent();
    path = new LinkedList<>();
    index = 0;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionGraphRoadModelImpl) model);
    Point p;
    do {
      p = model.getRandomPosition(rng);
    } while (roadModel.get().isOccupied(p));
    roadModel.get().addObjectAt(this, p);

  }

  @Override
  public double getSpeed() {
    return 1;
  }

  void nextDestination() {
    destination = Optional.of(roadModel.get().getRandomPosition(rng));
    path = new LinkedList<>(roadModel.get().getShortestPathTo(this,
      destination.get()));
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    if (!destination.isPresent()) {
      nextDestination();
    }

    try {
        if (path.size() > 1 && roadModel.get().isOccupied(path.get(1))) {
            Collection<Point> pts = roadModel.get().getGraph().getOutgoingConnections(roadModel.get().getPosition(this));
            for (Point pt : pts){
                if (!roadModel.get().isOccupied(pt)){
                    System.out.print("Test");
                    path.clear();
                    path.add(roadModel.get().getPosition(this));
                    path.add(pt);
                    LinkedList<Point> temp = new LinkedList<>(roadModel.get().getShortestPathTo(pt,
                            destination.get()));
                    path.addAll(temp);
                }
            }
            //return;
        }
        roadModel.get().followPath(this, path, timeLapse);
    } catch (DeadlockException e) {
      System.out.print("Deadlock");
//      path.clear();
//      nextDestination();
      return;
    }

    if (roadModel.get().getPosition(this).equals(destination.get())) {
      nextDestination();
    }
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

}
