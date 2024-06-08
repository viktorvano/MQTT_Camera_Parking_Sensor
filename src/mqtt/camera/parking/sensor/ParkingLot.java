package mqtt.camera.parking.sensor;

import java.util.ArrayList;

public class ParkingLot {
    public ArrayList<PixelPlace> pixelPlaces = new ArrayList<>();
    public String name;
    public boolean isFree = false;

    public ParkingLot(String name, ArrayList<PixelPlace> pixelPlaces)
    {
        this.name = name;
        this.pixelPlaces = pixelPlaces;
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
