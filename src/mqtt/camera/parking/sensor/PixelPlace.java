package mqtt.camera.parking.sensor;

public class PixelPlace {
    public int x;
    public int y;
    public PixelPlace(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Pixel Place: [" + this.x + ", " + this.y + "]";
    }
}
