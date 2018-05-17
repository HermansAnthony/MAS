package util;

import java.io.*;
import java.text.SimpleDateFormat;

public class Monitor {
    private String fileName;
    private static String outputDirectory = "logging";
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public Monitor(String _fileName){

        File directory = new File(outputDirectory);
        if (! directory.exists()){
            directory.mkdir();
        }

        fileName = outputDirectory + "/" +  _fileName + ".txt";
        // Delete the file if it is already present
        File tempFile = new File(fileName);
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
            out.println(convertTimeToString(startTime) + text);
        } catch (IOException e) {
            System.err.println("Error writing to file " + fileName + ".\n");
        }

    }

    private String convertTimeToString(long startTime){
        int seconds = (int) (startTime / 1000) % 60 ;
        int minutes = (int) ((startTime / (1000*60)) % 60);
        int hours   = (int) ((startTime / (1000*60*60)) % 24);
        String formattedTime = String.format("%02d:%02d:%02d",hours,minutes,seconds);
        return "[" + formattedTime + "]";
    }
}
