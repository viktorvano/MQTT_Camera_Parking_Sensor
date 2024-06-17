package mqtt.camera.parking.sensor;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Label;
import org.eclipse.paho.client.mqttv3.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

import static mqtt.camera.parking.sensor.AppParameters.*;
import static mqtt.camera.parking.sensor.ParkingLotsFile.*;
import static mqtt.camera.parking.sensor.StringFile.*;

public class Main extends Application implements WebcamListener {
    private Webcam webcam;
    private BufferedImage bufferedImage;
    private Image image;
    private ImageView imageView;
    private int parkingCount = 0;
    private final ObservableList<PixelPlace> parkingLotPixels = FXCollections.observableArrayList();
    private final Button buttonClearPixelList = new Button();
    private final Button buttonAddParkingLot = new Button();
    private final Button buttonRemoveLot = new Button();
    private final TextField textFieldLotName = new TextField();
    private final ObservableList<ParkingLot> parkingLots = FXCollections.observableArrayList();
    private int lastParkingCount = -1;
    private Label labelParkingCount;
    private ListView<ParkingLot> listViewParkingLots;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        createDirectoryIfNotExist("res");
        parkingLots.setAll(loadParkingLots());

        brokerAddress = loadStringFromFile("res" + fileSeparator + "mqtt_broker_address.txt", brokerAddress);
        clientId = loadStringFromFile("res" + fileSeparator + "mqtt_client_id.txt", clientId);
        topic = loadStringFromFile("res" + fileSeparator + "mqtt_topic.txt", topic);
        username = loadStringFromFile("res" + fileSeparator + "mqtt_username.txt", username);
        password = loadStringFromFile("res" + fileSeparator + "mqtt_password.txt", password);

        try{
            greyTolerance = Integer.parseInt(loadStringFromFile("res" + fileSeparator + "grey_tolerance.txt", String.valueOf(greyTolerance)));
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        try{
            blackThreshold = Integer.parseInt(loadStringFromFile("res" + fileSeparator + "black_threshold.txt", String.valueOf(blackThreshold)));
        }catch (Exception e)
        {
            e.printStackTrace();
        }


        List<Webcam> webcams = Webcam.getWebcams();
        ComboBox<Webcam> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(webcams);
        comboBox.setPromptText("Select Camera");
        comboBox.setLayoutX(10);
        comboBox.setLayoutY(500);
        comboBox.setOnAction(event -> {
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
            webcam = comboBox.getSelectionModel().getSelectedItem();
            if (webcam != null) {
                webcam.setViewSize(WebcamResolution.VGA.getSize());
                webcam.addWebcamListener(Main.this);
                webcam.open();
                updateImageView();
            }
        });

        if (!webcams.isEmpty()) {
            webcam = webcams.get(0);
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcam.addWebcamListener(Main.this);
            webcam.open();
        }

        imageView = new ImageView();
        imageView.setImage(image);
        imageView.setLayoutX(6);
        imageView.setLayoutY(6);

        imageView.setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();
            if (bufferedImage != null) {
                int pixelX = (int) x;
                int pixelY = (int) y;
                if (pixelX < bufferedImage.getWidth() && pixelY < bufferedImage.getHeight()) {
                    int rgb = bufferedImage.getRGB(pixelX, pixelY);
                    Color color = new Color(rgb);
                    PixelPlace pixelPlace = new PixelPlace(pixelX, pixelY);
                    parkingLotPixels.add(pixelPlace);
                    System.out.println("X: " + pixelX + ", Y: " + pixelY +
                            ", Red: " + color.getRed() +
                            ", Green: " + color.getGreen() +
                            ", Blue: " + color.getBlue());
                }
            }
        });


        labelParkingCount = new Label("Parking count: " + parkingCount);
        labelParkingCount.setLayoutX(420);
        labelParkingCount.setLayoutY(500);
        labelParkingCount.setFont(Font.font("Arial", 20));

        ListView<PixelPlace> listViewLotPixels = new ListView<>(parkingLotPixels);
        listViewLotPixels.setLayoutX(10);
        listViewLotPixels.setLayoutY(550);
        listViewLotPixels.setPrefSize(150, 130);

        listViewParkingLots = new ListView<>(parkingLots);
        listViewParkingLots.setLayoutX(420);
        listViewParkingLots.setLayoutY(550);
        listViewParkingLots.setPrefSize(150, 130);

        // Listen for changes in the list to update the button's disabled state
        parkingLotPixels.addListener((ListChangeListener<PixelPlace>) change -> {
            buttonClearPixelList.setDisable(parkingLotPixels.isEmpty());
            toggleButtonAddParkingLot();
        });

        buttonClearPixelList.setText("Clear Pixel List");
        buttonClearPixelList.setLayoutX(170);
        buttonClearPixelList.setLayoutY(550);
        buttonClearPixelList.setOnAction(event -> {
            parkingLotPixels.clear();
        });
        buttonClearPixelList.setDisable(true);

        buttonAddParkingLot.setText("Add Parking Lot =>");
        buttonAddParkingLot.setLayoutX(280);
        buttonAddParkingLot.setLayoutY(580);
        buttonAddParkingLot.setOnAction(event -> {
            String parkingLotName = textFieldLotName.getText();
            ArrayList<PixelPlace> pixelPlaces = new ArrayList<>();
            for(PixelPlace place : parkingLotPixels)
            {
                pixelPlaces.add(new PixelPlace(place.x, place.y));
            }

            parkingLots.add(new ParkingLot(parkingLotName, pixelPlaces));
            saveParkingLots(parkingLots);

            textFieldLotName.setText("");
            parkingLotPixels.clear();
        });
        buttonAddParkingLot.setDisable(true);

        buttonRemoveLot.setText("Remove\nParking\nLot");
        buttonRemoveLot.setLayoutX(580);
        buttonRemoveLot.setLayoutY(580);
        buttonRemoveLot.setOnAction(event -> {
            int index = listViewParkingLots.getSelectionModel().getSelectedIndex();
            if(index != -1)
            {
                parkingLots.remove(index);
                saveParkingLots(parkingLots);
            }
        });

        textFieldLotName.setLayoutX(170);
        textFieldLotName.setLayoutY(580);
        textFieldLotName.setPrefWidth(80);
        textFieldLotName.textProperty().addListener(event -> {
            toggleButtonAddParkingLot();
        });

        Pane pane = new Pane();
        pane.setPrefSize(650, 700);
        pane.setStyle("-fx-background-color: #7F7F7F");
        pane.getChildren().add(imageView);
        pane.getChildren().add(labelParkingCount);
        pane.getChildren().add(comboBox);
        pane.getChildren().add(listViewLotPixels);
        pane.getChildren().add(buttonClearPixelList);
        pane.getChildren().add(buttonAddParkingLot);
        pane.getChildren().add(buttonRemoveLot);
        pane.getChildren().add(textFieldLotName);
        pane.getChildren().add(listViewParkingLots);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("MQTT Camera Parking Sensor - " + version);
        primaryStage.show();

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(5000), event -> {
            checkParkingLots();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void checkParkingLots()
    {
        if(bufferedImage != null)
        {
            parkingCount = 0;
            for(ParkingLot parkingLot : parkingLots)
            {
                parkingLot.calculateIfParkingLotIsFree(bufferedImage, greyTolerance, blackThreshold);
                if(parkingLot.isFree())
                {
                    parkingCount++;
                }
            }

            labelParkingCount.setText("Parking count: " + parkingCount);
            listViewParkingLots.refresh();

            if(parkingCount != lastParkingCount)
            {
                publishMQTT();
                lastParkingCount = parkingCount;
            }
        }
    }

    private String encodeImageToBase64(BufferedImage image) {
        String base64Image = "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            base64Image = Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return base64Image;
    }

    private void publishMQTT()
    {
        int value = parkingCount;
        int qos = 2;

        try {
            MqttClient sampleClient = new MqttClient(brokerAddress, clientId);
            sampleClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    System.out.println("Message arrived. Topic: " + topic + " Message: " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Delivery complete: " + token.getMessageId());
                }
            });

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());

            System.out.println("Connecting to broker: " + brokerAddress);
            sampleClient.connect(connOpts);
            System.out.println("Connected");

            String content = Integer.toString(value); // Convert the integer value to string
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
            System.out.println("Message published");

            // Publish image as Base64
            if (bufferedImage != null) {
                String base64Image = encodeImageToBase64(bufferedImage);
                String imageTopic = "parking/sensor/image";
                MqttMessage imageMessage = new MqttMessage(base64Image.getBytes());
                imageMessage.setQos(qos);
                sampleClient.publish(imageTopic, imageMessage);
                System.out.println("Image message published");
            }

            sampleClient.disconnect();
            System.out.println("Disconnected");
        } catch (MqttException me) {
            System.out.println("Reason: " + me.getReasonCode());
            System.out.println("Message: " + me.getMessage());
            System.out.println("Localized: " + me.getLocalizedMessage());
            System.out.println("Cause: " + me.getCause());
            System.out.println("Exception: " + me);
            me.printStackTrace();
        }
    }

    private void toggleButtonAddParkingLot()
    {
        buttonAddParkingLot.setDisable(parkingLotPixels.isEmpty() || textFieldLotName.getText().isEmpty());
    }

    private void updateImageView() {
        bufferedImage = webcam.getImage();
        image = SwingFXUtils.toFXImage(bufferedImage, null);
        imageView.setImage(image);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.out.println("Closing Application.");
    }

    @Override
    public void webcamOpen(WebcamEvent webcamEvent) {

    }

    @Override
    public void webcamClosed(WebcamEvent webcamEvent) {

    }

    @Override
    public void webcamDisposed(WebcamEvent webcamEvent) {

    }

    @Override
    public void webcamImageObtained(WebcamEvent webcamEvent) {
        updateImageView();
    }

    private void setMonochromatic(BufferedImage bufferedImageMono)
    {
        int color;
        Color c;
        int red, green, blue;
        for(int width = 0; width < bufferedImageMono.getWidth(); width++)
            for(int height = 0; height < bufferedImageMono.getHeight(); height++) {
                c = new Color(bufferedImageMono.getRGB(width, height));
                red = c.getRed();
                green = c.getGreen();
                blue = c.getBlue();
                color = (red + green + blue) / 3;
                color = color*65536 + color*256 + color;
                bufferedImageMono.setRGB(width, height, color);
            }
    }

    private BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage bufferedImageReturn = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
        int color;
        for(int width = 0; width < bi.getWidth(); width++)
            for(int height = 0; height < bi.getHeight(); height++) {
                color = bi.getRGB(width, height);
                bufferedImageReturn.setRGB(width, height, color);
            }
        return bufferedImageReturn;
    }
}
