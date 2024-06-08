package mqtt.camera.parking.sensor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ParkingLot {
    public ArrayList<PixelPlace> pixelPlaces;
    public String name;
    private boolean isFree = false;

    public ParkingLot(String name, ArrayList<PixelPlace> pixelPlaces)
    {
        this.name = name;
        this.pixelPlaces = pixelPlaces;
    }

    public void calculateIfParkingLotIsFree(BufferedImage bufferedImage, int tolerance)
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
            int min = averageColor - tolerance;
            int max = averageColor + tolerance;

            // if any of these colors are off the tolerance range the parking lot if not free
            if(red > max || red < min
            || green > max || green < min
            || blue > max || blue < min)
            {
                free = false;
                break;
            }
        }

        this.isFree = free;
    }

    public boolean isFree()
    {
        return isFree;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Parking Lot " + this.name + " contains these pixels: ");
        for(PixelPlace pixelPlace : pixelPlaces)
        {
            stringBuilder.append("[" + pixelPlace.x + ", " + pixelPlace.y + "], ");
        }

        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        stringBuilder.deleteCharAt(stringBuilder.length()-1);

        return stringBuilder.toString();
    }
}
