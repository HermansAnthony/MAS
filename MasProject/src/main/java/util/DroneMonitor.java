package util;

import java.io.*;

// TODO maybe just delete this class and only use the TimeMonitor class?
public class DroneMonitor {
    private String fileName;
    private static String outputDirectory = "logging";

    public DroneMonitor(String fileName){

        File directory = new File(outputDirectory);
        if (! directory.exists()){
            directory.mkdir();
        }

        this.fileName = outputDirectory + "/" +  fileName + ".txt";
        // Delete the file if it is already present
        File tempFile = new File(this.fileName);
        if (tempFile.isFile()){
            tempFile.delete();
        }
    }

    public void writeToFile(long startTime, String text){
        try(
                FileWriter fw = new FileWriter(fileName, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)
            ) {
            out.println("["+ Utilities.convertTimeToString(startTime) + "]" + text);
        } catch (IOException e) {
            System.err.println("Error writing to file " + fileName + ".\n");
        }
    }

}
