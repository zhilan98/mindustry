package mindustry.entities.units;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DroneLogger {

    private static final String LOG_DIR = System.getProperty("user.home") + File.separator + "mindustry_logs";
    private static final String LOG_FILE_PATH = LOG_DIR + File.separator + "log.txt";
    private static String lastState = "";
    private static boolean isInitialized = false;


    private static void initLogger() {
        if (isInitialized) return;

        try {

            Path logDirPath = Paths.get(LOG_DIR);
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
                System.out.println("[DroneLogger] Created log directory: " + LOG_DIR);
            }


            Path logFilePath = Paths.get(LOG_FILE_PATH);
            if (!Files.exists(logFilePath)) {
                Files.createFile(logFilePath);
                System.out.println("[DroneLogger] Created log file: " + LOG_FILE_PATH);
            }


            writeSessionStart();
            isInitialized = true;

        } catch (IOException e) {
            System.err.println("[DroneLogger] Failed to initialize logger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void writeSessionStart() {
        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             PrintWriter out = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            out.println("=== New Session Started at " + timestamp + " ===");
            out.flush();

        } catch (IOException e) {
            System.err.println("[DroneLogger] Failed to write session start: " + e.getMessage());
        }
    }

    public static void logState(String state) {

        initLogger();


        if (state.equals(lastState)) return;
        lastState = state;

        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             PrintWriter out = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            out.println("[" + timestamp + "] State changed to " + state);
            out.flush();

            System.out.println("[DroneLogger] State changed to " + state);

        } catch (IOException e) {
            System.err.println("[DroneLogger] Failed to write log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void logMessage(String message) {
        initLogger();

        try (FileWriter fw = new FileWriter(LOG_FILE_PATH, true);
             PrintWriter out = new PrintWriter(fw)) {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            out.println("[" + timestamp + "] " + message);
            out.flush();

        } catch (IOException e) {
            System.err.println("[DroneLogger] Failed to write message: " + e.getMessage());
        }
    }


    public static String getLogFilePath() {
        return LOG_FILE_PATH;
    }


    public static void clearLog() {
        try {
            Files.deleteIfExists(Paths.get(LOG_FILE_PATH));
            isInitialized = false;
            System.out.println("[DroneLogger] Log file cleared");
        } catch (IOException e) {
            System.err.println("[DroneLogger] Failed to clear log: " + e.getMessage());
        }
    }

    public static boolean logFileExists() {
        return Files.exists(Paths.get(LOG_FILE_PATH));
    }


    public static void printLogContents() {
        try {
            if (Files.exists(Paths.get(LOG_FILE_PATH))) {
                System.out.println("=== Log File Contents ===");
                Files.lines(Paths.get(LOG_FILE_PATH)).forEach(System.out::println);
                System.out.println("=== End of Log ===");
            } else {
                System.out.println("[DroneLogger] Log file does not exist");
            }
        } catch (IOException e) {
            System.err.println("[DroneLogger] Failed to read log: " + e.getMessage());
        }
    }
}