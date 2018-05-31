import experiment.DroneExperiment;
import util.PropertiesLoader;

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
            DroneExperiment.run(scenarioIdentifier);
        }
    }

    private static void displayInformation() {
        System.out.println("Usage of program: java -jar <x.jar> <runMode> <experimentScenario>");
        System.out.println("\t<x.jar> The jar with dependencies.");
        System.out.println("\t<runMode> Which mode you would like to run (either \"Example\" or \"Experiment\".");
        System.out.println("\t<experimentScenario> If choosing to run the experiments, the scenario you would like to run (\"Default\", \"LW\", \"HW\").");
        System.out.println("Default values: <runMode>=\"Default\", <experimentScenario>=\"Default\".\n");
    }

}
