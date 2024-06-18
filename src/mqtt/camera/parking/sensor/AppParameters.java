package mqtt.camera.parking.sensor;

public class AppParameters {
    public static final String version = "v20240617";
    public static final String fileSeparator = System.getProperty("file.separator");
    public static String brokerAddress = "tcp://192.168.1.25:1883"; // Full Broker URL + port
    public static String clientId = "Parking Camera Sensor";
    public static String mqtt_sensor_topic = "parking/sensor";
    public static String mqtt_image_topic = "parking/sensor/image";
    public static String username = "default_mqtt_user";
    public static String password = "default_mqtt_password";
    public static int greyTolerance = 40;
    public static int blackThreshold = 60;
}
