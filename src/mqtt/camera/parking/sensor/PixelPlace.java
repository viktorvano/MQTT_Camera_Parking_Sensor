package mqtt.camera.parking.sensor;

import java.io.Serializable;

public class PixelPlace implements Serializable {
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
