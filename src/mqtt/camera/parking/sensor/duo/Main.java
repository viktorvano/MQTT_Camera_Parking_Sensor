package mqtt.camera.parking.sensor.duo;

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
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;

import static mqtt.camera.parking.sensor.duo.AppParameters.*;
import static mqtt.camera.parking.sensor.duo.ParkingLotsFile.*;
import static mqtt.camera.parking.sensor.duo.StringFile.*;

public class Main extends Application implements WebcamListener {
    private Webcam webcam;
    private Webcam webcam2;
    private BufferedImage bufferedImage;
    private BufferedImage bufferedImage2;
    private Image image;
    private Image image2;
    private ImageView imageView;
    private ImageView imageView2;
    private int parkingCount = 0;
    private int parkingCount2 = 0;
    private final ObservableList<PixelPlace> parkingLotPixels = FXCollections.observableArrayList();
    private final ObservableList<PixelPlace> parkingLotPixels2 = FXCollections.observableArrayList();
    private final Button buttonClearPixelList = new Button();
    private final Button buttonClearPixelList2 = new Button();
    private final Button buttonAddParkingLot = new Button();
    private final Button buttonAddParkingLot2 = new Button();
    private final Button buttonRemoveLot = new Button();
    private final Button buttonRemoveLot2 = new Button();
    private final TextField textFieldLotName = new TextField();
    private final TextField textFieldLotName2 = new TextField();
    private final ObservableList<ParkingLot> parkingLots = FXCollections.observableArrayList();
    private final ObservableList<ParkingLot> parkingLots2 = FXCollections.observableArrayList();
    private Label labelParkingCount;
    private Label labelParkingCount2;
    private ListView<ParkingLot> listViewParkingLots;
    private ListView<ParkingLot> listViewParkingLots2;
    private Timeline timeline;
    private MqttClient mqttClient;
    private MqttConnectOptions connOpts;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private final ComboBox<Webcam> comboBoxWebCams = new ComboBox<>();
    private final ComboBox<Webcam> comboBoxWebCams2 = new ComboBox<>();
    private static ListView<PixelPlace> listViewLotPixels;
    private static ListView<PixelPlace> listViewLotPixels2;

    private static boolean cameraToggle = false;
    private static String cameraName = "camera 1";
    private static String cameraName2 = "camera 2";
    private final Object imageLock = new Object();
    private final Object imageLock2 = new Object();

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        createDirectoryIfNotExist("res");
        parkingLots.setAll(loadParkingLots());
        parkingLots2.setAll(loadParkingLots2());

        brokerAddress = loadStringFromFile("res" + fileSeparator + "mqtt_broker_address.txt", brokerAddress);
        clientId = loadStringFromFile("res" + fileSeparator + "mqtt_client_id.txt", clientId);
        mqtt_sensor_topic = loadStringFromFile("res" + fileSeparator + "mqtt_sensor_topic.txt", mqtt_sensor_topic);
        mqtt_sensor_topic2 = loadStringFromFile("res" + fileSeparator + "mqtt_sensor_topic2.txt", mqtt_sensor_topic2);
        mqtt_image_topic = loadStringFromFile("res" + fileSeparator + "mqtt_image_topic.txt", mqtt_image_topic);
        mqtt_image_topic2 = loadStringFromFile("res" + fileSeparator + "mqtt_image_topic2.txt", mqtt_image_topic2);
        username = loadStringFromFile("res" + fileSeparator + "mqtt_username.txt", username);
        password = loadStringFromFile("res" + fileSeparator + "mqtt_password.txt", password);
        cameraName = loadStringFromFile("res" + fileSeparator + "camera1.txt", cameraName);
        cameraName2 = loadStringFromFile("res" + fileSeparator + "camera2.txt", cameraName2);

        logger.info("Application started.");

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

        try{
            whiteThreshold = Integer.parseInt(loadStringFromFile("res" + fileSeparator + "white_threshold.txt", String.valueOf(whiteThreshold)));
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        try{
            mqttUpdatePeriodInSeconds = Integer.parseInt(loadStringFromFile("res" + fileSeparator + "mqtt_update_period_in_seconds.txt", String.valueOf(mqttUpdatePeriodInSeconds)));
        }catch (Exception e)
        {
            e.printStackTrace();
        }


        List<Webcam> webcams = Webcam.getWebcams();
        comboBoxWebCams.getItems().addAll(webcams);
        comboBoxWebCams.setPromptText("Select Camera");
        comboBoxWebCams.setLayoutX(10);
        comboBoxWebCams.setLayoutY(500);
        comboBoxWebCams.setOnAction(event -> {
            if (webcam != null && webcam.isOpen()) {
                try{
                    webcam.removeWebcamListener(Main.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                webcam.close();
            }

            try
            {
                webcam = comboBoxWebCams.getSelectionModel().getSelectedItem();
                if (webcam != null) {
                    saveStringToFile("res" + fileSeparator + "camera1.txt", webcam.getName());
                    closeAllCameras();
                    try{
                        if(!webcam.getViewSize().equals(WebcamResolution.VGA.getSize()))
                        {
                            webcam.setViewSize(WebcamResolution.VGA.getSize());
                        }
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    cameraToggle = true;
                    keepOneCameraOpen();
                    updateImageView();
                }
            }catch (Exception e)
            {
                e.printStackTrace();
                fixWebcamStream();
            }
        });

        comboBoxWebCams2.getItems().addAll(webcams);
        comboBoxWebCams2.setPromptText("Select Camera2");
        comboBoxWebCams2.setLayoutX(10 + widthOffset2);
        comboBoxWebCams2.setLayoutY(500);
        comboBoxWebCams2.setOnAction(event -> {
            if (webcam2 != null && webcam2.isOpen()) {
                try{
                    webcam2.removeWebcamListener(Main.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                webcam2.close();
            }

            try
            {
                webcam2 = comboBoxWebCams2.getSelectionModel().getSelectedItem();
                if (webcam2 != null) {
                    saveStringToFile("res" + fileSeparator + "camera2.txt", webcam2.getName());
                    closeAllCameras();
                    try{
                        if(!webcam2.getViewSize().equals(WebcamResolution.VGA.getSize()))
                        {
                            webcam2.setViewSize(WebcamResolution.VGA.getSize());
                        }
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    cameraToggle = false;
                    keepOneCameraOpen();
                    updateImageView2();
                }
            }catch (Exception e)
            {
                e.printStackTrace();
                fixWebcamStream2();
            }
        });

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

        imageView2 = new ImageView();
        imageView2.setImage(image2);
        imageView2.setLayoutX(6 + widthOffset2);
        imageView2.setLayoutY(6);

        imageView2.setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();
            if (bufferedImage2 != null) {
                int pixelX = (int) x;
                int pixelY = (int) y;
                if (pixelX < bufferedImage2.getWidth() && pixelY < bufferedImage2.getHeight()) {
                    int rgb = bufferedImage2.getRGB(pixelX, pixelY);
                    Color color = new Color(rgb);
                    PixelPlace pixelPlace = new PixelPlace(pixelX, pixelY);
                    parkingLotPixels2.add(pixelPlace);
                    System.out.println("X: " + pixelX + ", Y: " + pixelY +
                            ", Red: " + color.getRed() +
                            ", Green: " + color.getGreen() +
                            ", Blue: " + color.getBlue());
                }
            }
        });


        labelParkingCount = new Label("Parking count B: " + parkingCount);
        labelParkingCount.setLayoutX(420);
        labelParkingCount.setLayoutY(500);
        labelParkingCount.setFont(Font.font("Arial", 20));

        labelParkingCount2 = new Label("Parking count C: " + parkingCount2);
        labelParkingCount2.setLayoutX(420 + widthOffset2);
        labelParkingCount2.setLayoutY(500);
        labelParkingCount2.setFont(Font.font("Arial", 20));

        listViewLotPixels = new ListView<>(parkingLotPixels);
        listViewLotPixels.setLayoutX(10);
        listViewLotPixels.setLayoutY(550);
        listViewLotPixels.setPrefSize(150, 130);

        listViewLotPixels2 = new ListView<>(parkingLotPixels2);
        listViewLotPixels2.setLayoutX(10 + widthOffset2);
        listViewLotPixels2.setLayoutY(550);
        listViewLotPixels2.setPrefSize(150, 130);

        listViewParkingLots = new ListView<>(parkingLots);
        listViewParkingLots.setLayoutX(420);
        listViewParkingLots.setLayoutY(550);
        listViewParkingLots.setPrefSize(150, 130);

        listViewParkingLots2 = new ListView<>(parkingLots2);
        listViewParkingLots2.setLayoutX(420 + widthOffset2);
        listViewParkingLots2.setLayoutY(550);
        listViewParkingLots2.setPrefSize(150, 130);

        // Listen for changes in the list to update the button's disabled state
        parkingLotPixels.addListener((ListChangeListener<PixelPlace>) change -> {
            buttonClearPixelList.setDisable(parkingLotPixels.isEmpty());
            toggleButtonAddParkingLot();
        });

        // Listen for changes in the list to update the button's disabled state
        parkingLotPixels2.addListener((ListChangeListener<PixelPlace>) change -> {
            buttonClearPixelList2.setDisable(parkingLotPixels2.isEmpty());
            toggleButtonAddParkingLot2();
        });

        buttonClearPixelList.setText("Clear Pixel List");
        buttonClearPixelList.setLayoutX(170);
        buttonClearPixelList.setLayoutY(550);
        buttonClearPixelList.setOnAction(event -> {
            parkingLotPixels.clear();
        });
        buttonClearPixelList.setDisable(true);

        buttonClearPixelList2.setText("Clear Pixel List");
        buttonClearPixelList2.setLayoutX(170 + widthOffset2);
        buttonClearPixelList2.setLayoutY(550);
        buttonClearPixelList2.setOnAction(event -> {
            parkingLotPixels2.clear();
        });
        buttonClearPixelList2.setDisable(true);

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

            FXCollections.sort(parkingLots, Comparator.comparing(ParkingLot::getName));

            saveParkingLots(parkingLots);

            textFieldLotName.setText("");
            parkingLotPixels.clear();
        });
        buttonAddParkingLot.setDisable(true);

        buttonAddParkingLot2.setText("Add Parking Lot =>");
        buttonAddParkingLot2.setLayoutX(280 + widthOffset2);
        buttonAddParkingLot2.setLayoutY(580);
        buttonAddParkingLot2.setOnAction(event -> {
            String parkingLotName = textFieldLotName2.getText();
            ArrayList<PixelPlace> pixelPlaces = new ArrayList<>();
            for(PixelPlace place : parkingLotPixels2)
            {
                pixelPlaces.add(new PixelPlace(place.x, place.y));
            }

            parkingLots2.add(new ParkingLot(parkingLotName, pixelPlaces));

            FXCollections.sort(parkingLots2, Comparator.comparing(ParkingLot::getName));

            saveParkingLots2(parkingLots2);

            textFieldLotName2.setText("");
            parkingLotPixels2.clear();
        });
        buttonAddParkingLot2.setDisable(true);

        buttonRemoveLot.setText("Remove\nParking\nLot");
        buttonRemoveLot.setLayoutX(580);
        buttonRemoveLot.setLayoutY(580);
        buttonRemoveLot.setOnAction(event -> {
            int index = listViewParkingLots.getSelectionModel().getSelectedIndex();
            if(index != -1)
            {
                parkingLots.remove(index);

                FXCollections.sort(parkingLots, Comparator.comparing(ParkingLot::getName));

                saveParkingLots(parkingLots);
            }
        });

        buttonRemoveLot2.setText("Remove\nParking\nLot");
        buttonRemoveLot2.setLayoutX(580 + widthOffset2);
        buttonRemoveLot2.setLayoutY(580);
        buttonRemoveLot2.setOnAction(event -> {
            int index = listViewParkingLots2.getSelectionModel().getSelectedIndex();
            if(index != -1)
            {
                parkingLots2.remove(index);

                FXCollections.sort(parkingLots2, Comparator.comparing(ParkingLot::getName));

                saveParkingLots2(parkingLots2);
            }
        });

        textFieldLotName.setLayoutX(170);
        textFieldLotName.setLayoutY(580);
        textFieldLotName.setPrefWidth(80);
        textFieldLotName.textProperty().addListener(event -> {
            toggleButtonAddParkingLot();
        });

        textFieldLotName2.setLayoutX(170 + widthOffset2);
        textFieldLotName2.setLayoutY(580);
        textFieldLotName2.setPrefWidth(80);
        textFieldLotName2.textProperty().addListener(event -> {
            toggleButtonAddParkingLot2();
        });

        Pane pane = new Pane();
        pane.setPrefSize(paneWidth, paneHeight);
        pane.setStyle("-fx-background-color: #7F7F7F");
        pane.getChildren().add(imageView);
        pane.getChildren().add(imageView2);
        pane.getChildren().add(labelParkingCount);
        pane.getChildren().add(labelParkingCount2);
        pane.getChildren().add(comboBoxWebCams);
        pane.getChildren().add(comboBoxWebCams2);
        pane.getChildren().add(listViewLotPixels);
        pane.getChildren().add(listViewLotPixels2);
        pane.getChildren().add(buttonClearPixelList);
        pane.getChildren().add(buttonClearPixelList2);
        pane.getChildren().add(buttonAddParkingLot);
        pane.getChildren().add(buttonAddParkingLot2);
        pane.getChildren().add(buttonRemoveLot);
        pane.getChildren().add(buttonRemoveLot2);
        pane.getChildren().add(textFieldLotName);
        pane.getChildren().add(textFieldLotName2);
        pane.getChildren().add(listViewParkingLots);
        pane.getChildren().add(listViewParkingLots2);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("MQTT Camera Parking Sensor - " + version);
        primaryStage.getIcons().add(new Image("/mqtt/camera/parking/sensor/duo/resources/icon.jpg"));
        primaryStage.show();

        timeline = new Timeline(new KeyFrame(Duration.seconds(mqttUpdatePeriodInSeconds), event -> {

            cameraToggle = !cameraToggle;
            keepOneCameraOpen();

            if(cameraToggle && webcam != null)
            {
                if(!webcam.isOpen())
                {
                    try {
                        webcam.open();
                    }catch (Exception e)
                    {
                        System.out.println("Cannot Open Camera1 in Timeline.");
                        e.printStackTrace();
                    }
                }
                else
                {
                    fixWebcamStream();
                }
                checkParkingLots();
            }


            if(!cameraToggle && webcam2 != null)
            {
                if(!webcam2.isOpen())
                {
                    try {
                        webcam2.open();
                    }catch (Exception e)
                    {
                        System.out.println("Cannot Open Camera2 in Timeline.");
                        e.printStackTrace();
                    }
                }
                else
                {
                    fixWebcamStream2();
                }
                checkParkingLots();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        selectFirstRandomCamera();
        selectFirstRandomCamera2();
    }

    private void selectFirstRandomCamera()
    {
        if (webcam != null && webcam.isOpen()) {
            try{
                webcam.removeWebcamListener(Main.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            webcam.close();
        }

        try
        {
            List<Webcam> webcams = Webcam.getWebcams();
            comboBoxWebCams.getItems().clear();
            comboBoxWebCams.getItems().addAll(webcams);

            int cameraIndex = -1;
            for(int i=0; i< webcams.size(); i++)
            {
                if(webcams.get(i).getName().equals(cameraName))
                {
                    cameraIndex = i;
                    break;
                }
            }
            if(cameraIndex != -1)
            {
                webcam = webcams.get(cameraIndex); // Select the webcam at the specific index
            }else{
                Random random = new Random();
                int randomIndex = random.nextInt(webcams.size()); // Get a random index
                webcam = webcams.get(randomIndex); // Select the webcam at the random index
            }
            comboBoxWebCams.getSelectionModel().select(webcam);
            if (webcam != null) {
                closeAllCameras();
                try{
                    if(!webcam.getViewSize().equals(WebcamResolution.VGA.getSize()))
                    {
                        webcam.setViewSize(WebcamResolution.VGA.getSize());
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                keepOneCameraOpen();
                updateImageView();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
            fixWebcamStream();
        }
    }

    private void selectFirstRandomCamera2()
    {
        if (webcam2 != null && webcam2.isOpen()) {
            try{
                webcam2.removeWebcamListener(Main.this);
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            webcam2.close();
        }

        try
        {
            List<Webcam> webcams = Webcam.getWebcams();
            comboBoxWebCams2.getItems().clear();
            comboBoxWebCams2.getItems().addAll(webcams);

            int cameraIndex = -1;
            for(int i=0; i< webcams.size(); i++)
            {
                if(webcams.get(i).getName().equals(cameraName2))
                {
                    cameraIndex = i;
                    break;
                }
            }
            if(cameraIndex != -1)
            {
                webcam2 = webcams.get(cameraIndex); // Select the webcam at the specific index
            }else{
                Random random = new Random();
                int randomIndex = random.nextInt(webcams.size()); // Get a random index
                webcam2 = webcams.get(randomIndex); // Select the webcam at the random index
            }
            comboBoxWebCams2.getSelectionModel().select(webcam2);
            if (webcam2 != null) {
                closeAllCameras();
                try{
                    if(!webcam2.getViewSize().equals(WebcamResolution.VGA.getSize()))
                    {
                        webcam2.setViewSize(WebcamResolution.VGA.getSize());
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                keepOneCameraOpen();
                updateImageView2();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
            fixWebcamStream2();
        }
    }

    private void fixWebcamStream()
    {
        try
        {
            closeAllCameras();

            List<Webcam> webcams = Webcam.getWebcams();
            comboBoxWebCams.getItems().clear();
            comboBoxWebCams.getItems().addAll(webcams);

            int cameraIndex = -1;
            for(int i=0; i< webcams.size(); i++)
            {
                if(webcams.get(i).getName().equals(cameraName))
                {
                    cameraIndex = i;
                    break;
                }
            }
            if(cameraIndex != -1)
            {
                webcam = webcams.get(cameraIndex); // Select the webcam at the specific index
            }else{
                Random random = new Random();
                int randomIndex = random.nextInt(webcams.size()); // Get a random index
                webcam = webcams.get(randomIndex); // Select the webcam at the random index
            }
            if (webcam.isOpen()) {
                webcam.close();
                int waitMs = 0;
                while (webcam.isOpen() && waitMs < 2000) {
                    try{
                        Thread.sleep(50);
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    waitMs += 50;
                }
            }
            comboBoxWebCams.getSelectionModel().select(webcam);
            if (webcam != null) {
                try{
                    if(!webcam.getViewSize().equals(WebcamResolution.VGA.getSize()))
                    {
                        webcam.setViewSize(WebcamResolution.VGA.getSize());
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                keepOneCameraOpen();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void fixWebcamStream2()
    {
        try
        {
            closeAllCameras();

            List<Webcam> webcams = Webcam.getWebcams();
            comboBoxWebCams2.getItems().clear();
            comboBoxWebCams2.getItems().addAll(webcams);

            int cameraIndex = -1;
            for(int i=0; i< webcams.size(); i++)
            {
                if(webcams.get(i).getName().equals(cameraName2))
                {
                    cameraIndex = i;
                    break;
                }
            }
            if(cameraIndex != -1)
            {
                webcam2 = webcams.get(cameraIndex); // Select the webcam at the specific index
            }else{
                Random random = new Random();
                int randomIndex = random.nextInt(webcams.size()); // Get a random index
                webcam2 = webcams.get(randomIndex); // Select the webcam at the random index
            }
            if (webcam2.isOpen()) {
                webcam2.close();
                int waitMs = 0;
                while (webcam2.isOpen() && waitMs < 2000) {
                    try{
                        Thread.sleep(50);
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    waitMs += 50;
                }
            }
            comboBoxWebCams2.getSelectionModel().select(webcam2);
            if (webcam2 != null) {
                try{
                    if(!webcam2.getViewSize().equals(WebcamResolution.VGA.getSize()))
                    {
                        webcam2.setViewSize(WebcamResolution.VGA.getSize());
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                keepOneCameraOpen();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void checkParkingLots()
    {
        if(bufferedImage != null)
        {
            parkingCount = 0;
            for(ParkingLot parkingLot : parkingLots)
            {
                parkingLot.calculateIfParkingLotIsFree(bufferedImage, greyTolerance, blackThreshold, whiteThreshold);
                if(parkingLot.isFree())
                {
                    parkingCount++;
                }
            }

            labelParkingCount.setText("Parking count B: " + parkingCount);
            listViewParkingLots.refresh();

            publishMQTT();
        }

        if(bufferedImage2 != null)
        {
            parkingCount2 = 0;
            for(ParkingLot parkingLot : parkingLots2)
            {
                parkingLot.calculateIfParkingLotIsFree(bufferedImage2, greyTolerance, blackThreshold, whiteThreshold);
                if(parkingLot.isFree())
                {
                    parkingCount2++;
                }
            }

            labelParkingCount2.setText("Parking count C: " + parkingCount2);
            listViewParkingLots2.refresh();

            publishMQTT();
        }
        //System.gc();
    }

    private String encodeImageToBase64(BufferedImage image) {
        String base64Image = "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            base64Image = Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }/*finally {
            image.flush();
        }*/
        return base64Image;
    }

    private void initializeMQTTClient() throws MqttException {
        mqttClient = new MqttClient(brokerAddress, clientId, new MemoryPersistence());
        mqttClient.setCallback(new MqttCallback() {
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

        connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setUserName(username);
        connOpts.setPassword(password.toCharArray());
        connOpts.setAutomaticReconnect(true);
    }

    private void resolveMQTT() throws MqttException {
        if (mqttClient == null || !mqttClient.isConnected())//runs only when needed
        {
            if(mqttClient == null)
            {
                System.out.println("Initializing MQTT: " + brokerAddress);
                initializeMQTTClient();
            }
            if(!mqttClient.isConnected())
            {
                mqttClient.connect(connOpts);
                System.out.println("MQTT Client Connected");
            }
        }
    }

    private void publishMQTT()
    {
        int qos = 2;

        try {
            String content = Integer.toString(parkingCount); // Convert the integer value to string
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            resolveMQTT();
            mqttClient.publish(mqtt_sensor_topic, message);
            System.out.println("Parking B Message published");

            // Publish image B
            synchronized (imageLock) {
                if (bufferedImage != null)
                {
                    String base64Image = encodeImageToBase64(bufferedImage);
                    MqttMessage imageMessage = new MqttMessage(base64Image.getBytes());
                    imageMessage.setQos(qos);
                    resolveMQTT();
                    mqttClient.publish(mqtt_image_topic, imageMessage);
                    System.out.println("Image B message published");
                } else
                {
                    System.out.println("Image B is null...");
                }
            }

            content = Integer.toString(parkingCount2); // Convert the integer value to string
            message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            resolveMQTT();
            mqttClient.publish(mqtt_sensor_topic2, message);
            System.out.println("Parking C Message published");

            // Publish image C
            synchronized (imageLock2) {
                if (bufferedImage2 != null)
                {
                    String base64Image = encodeImageToBase64(bufferedImage2);
                    MqttMessage imageMessage = new MqttMessage(base64Image.getBytes());
                    imageMessage.setQos(qos);
                    resolveMQTT();
                    mqttClient.publish(mqtt_image_topic2, imageMessage);
                    System.out.println("Image C message published");
                } else
                {
                    System.out.println("Image C is null...");
                }
            }
        } catch (MqttException me) {
            System.out.println("Reason: " + me.getReasonCode());
            System.out.println("Message: " + me.getMessage());
            System.out.println("Localized: " + me.getLocalizedMessage());
            System.out.println("Cause: " + me.getCause());
            System.out.println("Exception: " + me);
            me.printStackTrace();
            try {
                if (mqttClient != null)
                {
                    mqttClient.disconnect();
                    mqttClient.close();
                }
                System.out.println("Disconnected");
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
        System.out.println("Total memory: " + Runtime.getRuntime().totalMemory());
    }

    private void toggleButtonAddParkingLot()
    {
        buttonAddParkingLot.setDisable(parkingLotPixels.isEmpty() || textFieldLotName.getText().isEmpty());
    }

    private void toggleButtonAddParkingLot2()
    {
        buttonAddParkingLot2.setDisable(parkingLotPixels2.isEmpty() || textFieldLotName2.getText().isEmpty());
    }

    private void updateImageView() {
        if(webcam != null)
        {
            if (bufferedImage != null) {
                bufferedImage.flush(); // Release previous image resources
            }
            keepOneCameraOpen();
            bufferedImage = webcam.getImage();
            image = SwingFXUtils.toFXImage(bufferedImage, null);
            imageView.setImage(image);
        }else
        {
            fixWebcamStream();
        }
    }

    private void updateImageView2() {
        if(webcam2 != null)
        {
            if (bufferedImage2 != null) {
                bufferedImage2.flush(); // Release previous image resources
            }
            keepOneCameraOpen();
            bufferedImage2 = webcam2.getImage();
            image2 = SwingFXUtils.toFXImage(bufferedImage2, null);
            imageView2.setImage(image2);
        }else
        {
            fixWebcamStream2();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        try {
            if (mqttClient != null)
            {
                mqttClient.disconnect();
                mqttClient.close();
            }
            System.out.println("Disconnected");
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if (timeline != null) {
            timeline.stop();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.removeWebcamListener(Main.this);
            webcam.close();
        }
        if (webcam2 != null && webcam2.isOpen()) {
            webcam2.removeWebcamListener(Main.this);
            webcam2.close();
        }
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
        if(cameraToggle)
        {
            updateImageView();
        }else
        {
            updateImageView2();
        }
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

    private void closeAllCameras()
    {
        if(webcam != null && webcam.isOpen())
        {
            webcam.removeWebcamListener(Main.this);
            webcam.close();
        }

        if(webcam2 != null && webcam2.isOpen())
        {
            webcam2.removeWebcamListener(Main.this);
            webcam2.close();
        }
    }

    private void keepOneCameraOpen()
    {
        if(cameraToggle)
        {
            if(webcam2 != null && webcam2.isOpen())
            {
                try{
                    webcam2.removeWebcamListener(Main.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                webcam2.close();
            }

            if(webcam != null && !webcam.isOpen())
            {
                try{
                    webcam.addWebcamListener(Main.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                webcam.open();
            }
        }else
        {
            if(webcam != null && webcam.isOpen())
            {
                try{
                    webcam.removeWebcamListener(Main.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                webcam.close();
            }

            if(webcam2 != null && !webcam2.isOpen())
            {
                try{
                    webcam2.addWebcamListener(Main.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                webcam2.open();
            }
        }
    }
}
