package mindustry.entities.units;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
public class DroneLogger {
    private static final String logFilePath = "log.txt";
    private static String lastState = "";

    public static void logState(String state){
        if(state.equals(lastState)) return;
        lastState = state;
        try(FileWriter fw = new FileWriter(logFilePath, true);
            PrintWriter out = new PrintWriter(fw)) {
            out.println("State changed to " + state);
        } catch(IOException e){
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }
}