# Multi-Agent Systems: Pickup and delivery: Drone Grocery Delivery

For the multi-agent systems course at KULeuven we chose to implement / make a case study of delivering groceries with drones. We used two types of drones namely a lightweight drone and a heavy weight. A lightweight drone is capable of carrying a maximum load of 3500 grams. A lightweight drone is capable of carrying a maximum load of 9000 grams. They travel with a speed that is dependent on the current load.


### Prerequisites

- [Maven](https://maven.apache.org/)

### Installing

For the installation part we refer to the [RinSim Installation Page](http://rinsim.rinde.nl/installation/).

## How to use our code

Usage of program: `java -jar <x.jar> <runMode> <experimentScenario> <configurationFile>`
* `<x.jar>` The jar with dependencies.
* `<runMode>` Which mode you would like to run (either "Example" or "Experiment".
* `<experimentScenario>` When running the experiments, the scenario you would like to run ("Default", "LW", "HW").
* `<configurationFile>` The path to a custom configuration file.
Default values: <runMode>="Example", <experimentScenario>="Default". 

If you would like to run the simulation with a custom configuration file, you should run it as follows `java -cp <path_to_configuration_file>:path_to_jar.jar RunClass <runMode> <experimentScenario> <configurationFile>`. The `<path_to_configuration_file>` should be filled in as the directory where the configuration file is situated.

### Running the different experiments

We provided three types of experiments:
- The default scenario consisting of 10 LW drones and 20 HW drones.
- Only LW drones.
- Only HW drones.

Our experiments will write the results to a file in the experiments folder. They will also be printed in the console. An example of the output can be:
```
10 lightweight drones were added.
20 heavyweight drones were added.
Average charging station occupation: <Lightweight: 31,86%, Heavyweight: 71,97%>.
Average delivery time: 00:04:29.
Average time overdue: 00:19:58.
Amount of delivered orders: 1309.
Amount of overdue orders: 346.
Orders delivered on time: 79,09%.
Simulation has completed.
```

## Built With

* [RinSim](https://github.com/rinde/RinSim) - Logistics simulator written in Java.
* [Maven](https://maven.apache.org/) - Dependency Management
* [IntelliJ](https://www.jetbrains.com/idea/) - Java integrated development environment (IDE)

## Authors

* **Anthony Hermans** [[GitHub]](https://github.com/HermansAnthony)
* **Federico Quin** [[GitHub]](https://github.com/FedericoQuin)
