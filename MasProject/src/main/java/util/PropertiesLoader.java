package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    public static String propertiesFileLocation = "/config.properties";
    private static PropertiesLoader instance = null;
    private Properties properties;

    private PropertiesLoader() {
        properties = new Properties();
        InputStream inputStream = getClass().getResourceAsStream(propertiesFileLocation);
        if (inputStream == null) {
            System.err.println("Could not load the properties file, file not found.");
        }

        try {
            properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("Could not load the properties file.");
        }
    }

    public static PropertiesLoader getInstance() {
        if (instance == null) {
            instance = new PropertiesLoader();
        }
        return instance;
    }

    public String getMapLocation() {
        return properties.getProperty("map");
    }

    public String getStoresLocation() {
        return properties.getProperty("stores");
    }

    public int getTickLength() {
        return Integer.valueOf(properties.getProperty("tickLength"));
    }

    public int getMapSize() {
        return Integer.valueOf(properties.getProperty("mapSize"));
    }

    public int getServiceDuration() {
        return Integer.valueOf(properties.getProperty("serviceDuration"));
    }

    public int getLowerBoundDeliveryTime() {
        return Integer.valueOf(properties.getProperty("lowerBoundDeliveryTime"));
    }

    public int getDeliveryTimeVariance() {
        return Integer.valueOf(properties.getProperty("deliveryTimeVariance"));
    }

    public Range getSpeedRangeLW() {
        return new Range(Integer.valueOf(properties.getProperty("minSpeedDroneLW")),
            Integer.valueOf(properties.getProperty("maxSpeedDroneLW")));
    }

    public Range getSpeedRangeHW() {
        return new Range(Integer.valueOf(properties.getProperty("minSpeedDroneHW")),
            Integer.valueOf(properties.getProperty("maxSpeedDroneHW")));
    }

    public int getCapacityLW() {
        return Integer.valueOf(properties.getProperty("capacityDroneLW"));
    }

    public int getCapacityHW() {
        return Integer.valueOf(properties.getProperty("capacityDroneHW"));
    }

    public int getBatteryLW() {
        return Integer.valueOf(properties.getProperty("batteryDroneLW"));
    }

    public int getBatteryHW() {
        return Integer.valueOf(properties.getProperty("batteryDroneHW"));
    }

    public String getProperty(String property) {
        return properties.getProperty(property);
    }

    public boolean propertyPresent(String property) {
        return properties.getProperty(property) != null;
    }
}
