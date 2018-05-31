package util;

import java.io.*;

// Monitor class (using the Singleton pattern) that writes the order descriptions to a file
// These descriptions will be used in the ExperimentPostProcessor
public class TimeMonitor {
    private static TimeMonitor singleton = new TimeMonitor();
    private static String onTime;
    private static String overDue;

    /* A private Constructor prevents any other
     * class from instantiating.
     */
    private TimeMonitor() {
        File directory = new File("logging");
        if (! directory.exists()){
            directory.mkdir();
        }

        this.onTime = "logging/ordersOnTime.csv";
        this.overDue = "logging/ordersOverdue.csv";
        // Delete the file if it is already present
        File tempFile = new File(this.onTime);
        if (tempFile.isFile()){
            tempFile.delete();
        }
        tempFile = new File(this.overDue);
        if (tempFile.isFile()){
            tempFile.delete();
        }
    }

    public static TimeMonitor getInstance( ) {
        return singleton;
    }

    // Write the announcement time and delivery time to a file
    public static void writeToFile(long announcement, long delivery, boolean orderOnTime) {
        String fileName = onTime;
        if (!orderOnTime)
            fileName = overDue;
        try(
            FileWriter fw = new FileWriter(fileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)
        ) {
            String description = String.valueOf(announcement) + ',' + String.valueOf(delivery);
            out.println(description);
        } catch (IOException e) {
            System.err.println("Error writing information to file " + fileName + ".\n");
        }
    }

    public static void clearFile() {
        File file = new File(onTime);
        file.delete();
        file = new File(overDue);
        file.delete();
    }

}
