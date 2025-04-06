package mqtt.camera.parking.sensor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;

public class ParkingLot implements Serializable {
    public ArrayList<PixelPlace> pixelPlaces;
    public String name;
    private boolean isFree = false;

    public ParkingLot(String name, ArrayList<PixelPlace> pixelPlaces)
    {
        this.name = name;
        this.pixelPlaces = pixelPlaces;
    }

    public void calculateIfParkingLotIsFree(BufferedImage bufferedImage, int greyTolerance, int blackThreshold, int whiteThreshold)
    {
        boolean free = true;
        int averageColor;
        Color c;
        int red, green, blue;
        for(PixelPlace place : pixelPlaces)
        {
            // verify if all the pixels are grey within the tolerance
            c = new Color(bufferedImage.getRGB(place.x, place.y));
            red = c.getRed();
            green = c.getGreen();
            blue = c.getBlue();
            averageColor = (red + green + blue) / 3;
            int min = averageColor - greyTolerance;
            int max = averageColor + greyTolerance;

            // if any of these colors are off the tolerance range the parking lot if not free
            if(red > max || red < min
            || green > max || green < min
            || blue > max || blue < min
            || red < blackThreshold// or of any point is too dark
            || green < blackThreshold
            || blue < blackThreshold
            ||
            (red > whiteThreshold// or of any point is too white
            && green > whiteThreshold
            && blue > whiteThreshold))
            {
                free = false;
                break;
            }
        }

        if(bufferedImage != null)
            bufferedImage.flush();

        this.isFree = free;
    }

    public boolean isFree()
    {
        return this.isFree;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if(this.isFree)
        {
            stringBuilder.append(this.name + " is FREE.");
        }else
        {
            stringBuilder.append(this.name + " is OCCUPIED.");
        }

        return stringBuilder.toString();
    }
}
