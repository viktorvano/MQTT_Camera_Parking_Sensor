package mqtt.camera.parking.sensor.duo;

public class AppParameters {
    public static final String version = "v20260109";
    public static final String fileSeparator = System.getProperty("file.separator");
    public static String brokerAddress = "tcp://192.168.1.25:1883"; // Full Broker URL + port
    public static String clientId = "Parking Camera Sensor";
    public static String mqtt_sensor_topic = "parking/sensor2";
    public static String mqtt_image_topic = "parking/sensor2/image2";
    public static String mqtt_sensor_topic2 = "parking/sensor3";
    public static String mqtt_image_topic2 = "parking/sensor3/image3";
    public static String username = "default_mqtt_user";
    public static String password = "default_mqtt_password";
    public static int greyTolerance = 40;
    public static int blackThreshold = 60;
    public static int whiteThreshold = 250;
    public static int mqttUpdatePeriodInSeconds = 15;
    public static final int paneWidth = 1300;
    public static final int paneHeight = 700;
    public static final int widthOffset2 = 650;
}
