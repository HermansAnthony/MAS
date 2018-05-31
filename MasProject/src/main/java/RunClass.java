import experiment.DroneExperiment;

public class RunClass {
    public static void main(String[] args) {
        displayInformation();

        String mode = "example";
        String scenarioIdentifier = "default";

        if (args.length > 1) {
            mode = args[0].toLowerCase();
        }
        if (args.length > 2) {
            scenarioIdentifier = args[1].toLowerCase();
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
        System.out.println("\t<experimentScenario> If choosing to run the experiments, the scenario you would like to run (\"Default\", \"LW\", \"HW\").\n");
    }

}
