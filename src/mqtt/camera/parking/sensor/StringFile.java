package mqtt.camera.parking.sensor;

import java.io.*;

public class StringFile {
    public static void saveStringToFile(String filename, String value)
    {
        try
        {
            File file = new File(filename);
            file.createNewFile();
            //Write Content
            FileWriter writer = new FileWriter(file);
            writer.write(value);
            writer.close();
        }catch (Exception e)
        {
            System.out.println("Failed to create the \"" + filename + "\" file.");
        }
    }

    public static String loadStringFromFile(String filename, String defaultValue)
    {
        StringBuilder content = new StringBuilder();
        try {
            File file = new File(filename);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Failed to read the \"" + filename + "\" file.");
            content = new StringBuilder(defaultValue);
            saveStringToFile(filename, defaultValue);
        }

        content.deleteCharAt(content.length()-1);// remove the last \n

        return content.toString();
    }
}
