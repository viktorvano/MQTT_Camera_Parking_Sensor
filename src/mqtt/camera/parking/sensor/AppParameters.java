package mqtt.camera.parking.sensor;

public class AppParameters {
    public static final String version = "v20240608";
    public static final String fileSeparator = System.getProperty("file.separator");
    public static String brokerAddress = "tcp://192.168.1.25:1883"; // Full Broker URL + port
    public static String clientId = "Parking Camera Sensor";
    public static String topic = "parking/sensor";
    public static String username = "default_mqtt_user";
    public static String password = "default_mqtt_password";
}