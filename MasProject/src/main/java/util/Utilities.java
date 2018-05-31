package util;

import com.github.rinde.rinsim.geom.Point;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Utilities {
    /**
     * Load the resolutionImage of the given image.
     * @param filename the image file.
     */
    public static Point loadResolutionImage(String filename) {
        try {
            BufferedImage bufferedImage = ImageIO.read(Utilities.class.getResource(filename));
            return new Point(bufferedImage.getWidth(), bufferedImage.getHeight());
        } catch (IOException e) {
            return new Point(1120.0,956.0);
        }
    }


    /**
     * Reads the store locations from the specified csv file.
     * @param filename the csv file.
     */
    public final static List<Point> loadStoreLocations(String filename) {
        List <Point> storeLocations = new ArrayList<>();
        try {
            InputStream in = Utilities.class.getResourceAsStream(filename);

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("[,\n]");
            while (scanner.hasNext()) {
                storeLocations.add(new Point(new Double(scanner.next()), new Double(scanner.next())));
            }
            scanner.close();
            in.close();
        } catch (IOException e) {
            System.err.println("Could not read store locations from csv file.");
        }
        return storeLocations;
    }

    /**
     * Converts the time to a readable string
     * @param time the time which needs to be converted to a readable format.
     */
    public final static String convertTimeToString(long time){
        int seconds = (int) (time / 1000) % 60 ;
        int minutes = (int) ((time / (1000*60)) % 60);
        int hours   = (int) ((time / (1000*60*60)) % 24);
        return String.format("%02d:%02d:%02d",hours,minutes,seconds);
    }
}
