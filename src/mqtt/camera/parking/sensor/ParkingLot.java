package mqtt.camera.parking.sensor;

public class ParkingLot {
    public int x;
    public int y;
    public int red;
    public int green;
    public int blue;
    public boolean isFree = false;

    public ParkingLot(int x, int y, int red, int green, int blue)
    {
        this.x = x;
        this.y = y;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public String toString() {
        return "X: " + x + ", Y: " + y +
                ", Red: " + red + ", Green: " + green + ", Blue: " + blue;
    }
}
