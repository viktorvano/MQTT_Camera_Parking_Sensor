package mqtt.camera.parking.sensor;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Label;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Main extends Application implements WebcamListener {
    private static final String version = "v20240606";
    private Webcam webcam;
    private BufferedImage bufferedImage;
    private Image image;
    private ImageView imageView;
    private int parkingCount = 0;
    private ObservableList<ParkingLot> parkingLotList = FXCollections.observableArrayList();

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
                    ParkingLot parkingLot = new ParkingLot(pixelX, pixelY, color.getRed(), color.getGreen(), color.getBlue());
                    parkingLotList.add(parkingLot);
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

        ListView<ParkingLot> listView = new ListView<>(parkingLotList);
        listView.setLayoutX(10);
        listView.setLayoutY(550);
        listView.setPrefSize(630, 100);

        Pane pane = new Pane();
        pane.setPrefSize(650, 700);
        pane.setStyle("-fx-background-color: #7F7F7F");
        pane.getChildren().add(imageView);
        pane.getChildren().add(labelParkingCount);
        pane.getChildren().add(comboBox);
        pane.getChildren().add(listView);
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
