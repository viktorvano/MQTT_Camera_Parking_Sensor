package mqtt.camera.parking.sensor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;

public class ParkingLotsFile {
    public static void createDirectoryIfNotExist(String directoryName)
    {
        File file = new File(directoryName);
        if(file.mkdir())
            System.out.println("New directory \"" + directoryName + "\" was created.");
        else
            System.out.println("Directory \"" + directoryName + "\" already exists.");
    }

    public static void saveParkingLots(ObservableList<ParkingLot> parkingLots)
    {
        try
        {
            String fileSeparator = System.getProperty("file.separator");
            File file = new File("res" + fileSeparator + "parkingLots.dat");
            file.createNewFile();
            FileOutputStream f = new FileOutputStream(file);
            ObjectOutputStream o = new ObjectOutputStream(f);
            for(int i=0; i<parkingLots.size(); i++)
                o.writeObject(parkingLots.get(i));
            o.close();
            f.close();
        }catch (Exception e)
        {
            System.out.println("Failed to create the \"parkingLots.dat\" file.");
        }
    }

    public static ObservableList<ParkingLot> loadParkingLots()
    {
        ObservableList<ParkingLot> parkingLots = FXCollections.observableArrayList();
        try
        {
            String fileSeparator = System.getProperty("file.separator");
            FileInputStream fi = new FileInputStream("res" + fileSeparator + "parkingLots.dat");
            ObjectInputStream oi = new ObjectInputStream(fi);
            Object object;
            while(true)
            {
                try{
                    object = oi.readObject();
                }
                catch(IOException e){
                    break;
                }
                if(object != null)
                    parkingLots.add((ParkingLot) object);
            }

            oi.close();
            fi.close();
        }catch (Exception e)
        {
            System.out.println("Failed to read the \"parkingLots.dat\" file.");
        }
        return parkingLots;
    }
}
