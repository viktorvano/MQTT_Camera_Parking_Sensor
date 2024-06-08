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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application implements WebcamListener {
    private static final String version = "v20240608";
    private Webcam webcam;
    private BufferedImage bufferedImage;
    private Image image;
    private ImageView imageView;
    private int parkingCount = 0;
    private final ObservableList<PixelPlace> parkingLotPixels = FXCollections.observableArrayList();
    private final Button buttonClearPixelList = new Button();
    private final Button buttonAddParkingLot = new Button();
    private final TextField textFieldLotName = new TextField();
    private final ObservableList<ParkingLot> parkingLots = FXCollections.observableArrayList();

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
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


        Label labelParkingCount = new Label("Parking count: " + parkingCount);
        labelParkingCount.setLayoutX(420);
        labelParkingCount.setLayoutY(500);
        labelParkingCount.setFont(Font.font("Arial", 20));

        ListView<PixelPlace> listViewLotPixels = new ListView<>(parkingLotPixels);
        listViewLotPixels.setLayoutX(10);
        listViewLotPixels.setLayoutY(550);
        listViewLotPixels.setPrefSize(150, 130);

        ListView<ParkingLot> listViewParkingLots = new ListView<>(parkingLots);
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

            textFieldLotName.setText("");
            parkingLotPixels.clear();
        });
        buttonAddParkingLot.setDisable(true);

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
        pane.getChildren().add(textFieldLotName);
        pane.getChildren().add(listViewParkingLots);
        Scene scene = new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("MQTT Camera Parking Sensor - " + version);
        primaryStage.show();

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            labelParkingCount.setText("Parking count: " + parkingCount);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
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
