import experiment.DroneExperiment;
import util.PropertiesLoader;

import java.util.stream.IntStream;

public class RunClass {
    public static void main(String[] args) {
        if (args.length == 0) {
            displayInformation();
        }

        String mode = "example";
        String scenarioIdentifier = "default";

        if (args.length > 0) {
            mode = args[0].toLowerCase();
        }
        if (args.length > 1) {
            scenarioIdentifier = args[1].toLowerCase();
        }
        if (args.length > 2) {
            PropertiesLoader.propertiesFileLocation = args[2];
        }

        if (mode.equals("example")) {
            DroneExample.run(false);
        } else if (mode.equals("experiment")) {
            PropertiesLoader loader = PropertiesLoader.getInstance();

            if (loader.propertyPresent("Experiment.seedRangeMin")) {
                int seedMin = Integer.valueOf(loader.getProperty("Experiment.seedRangeMin"));
                int seedMax = Integer.valueOf(loader.getProperty("Experiment.seedRangeMax"));

                final String finalScenarioIdentifier = scenarioIdentifier;

                IntStream.range(seedMin, seedMax + 1).forEach(i -> {
                    String name = "Scenario_" + finalScenarioIdentifier + "_seed" + i;
                    DroneExperiment.run(finalScenarioIdentifier, name, i);
                });
            } else {
                int seed = Integer.valueOf(loader.getProperty("Experiment.seed"));
                String name = "Scenario_" + scenarioIdentifier + "_seed" + seed;
                DroneExperiment.run(scenarioIdentifier, name, seed);
            }
        }
    }

    private static void displayInformation() {
        System.out.println("Usage of program: java -jar <x.jar> <runMode> <experimentScenario> <configurationFile>");
        System.out.println("\t<x.jar> The jar with dependencies.");
        System.out.println("\t<runMode> Which mode you would like to run (either \"Example\" or \"Experiment\".");
        System.out.println("\t<experimentScenario> When running the experiments, the scenario you would like to run (\"Default\", \"LW\", \"HW\").");
        System.out.println("\t<configurationFile> The path to a custom configuration file.");
        System.out.println("Default values: <runMode>=\"Example\", <experimentScenario>=\"Default\".\n");
    }

}
