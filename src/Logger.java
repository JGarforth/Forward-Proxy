/**
 * Logger generates a log file (Called server_log.txt by default in config).
 * Logs can be either INFO, ERROR, or DEBUG.
 * Call necessary method for those messages.
 * Hopefully thread safe.
 * */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Logger {

    private static String logFilePath;
    private static final Lock lock = new ReentrantLock();
    private enum LogLevel {INFO, ERROR, DEBUG}

    public static void initializeLogger (String logFilePath) {
        Logger.logFilePath = logFilePath;

        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            try {
                if (logFile.createNewFile()) {
                    logInfo("Log file created");
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to generate log file.");
            }
        }
    }

    public static void logInfo (String message) {
        log (LogLevel.INFO, message);
    }

    public static void logError (String message) {
        log (LogLevel.ERROR, message);
    }

    public static void logDebug (String message) {
        log (LogLevel.DEBUG, message);
    }

    private static void log(LogLevel level, String message) {
        lock.lock();
        try {
            SimpleDateFormat currTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = currTime.format(new Date());

            String logEntry = String.format("[%s] [%s] %s%n", timestamp, level, message);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
                writer.write(logEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } finally {
            lock.unlock();
        }
    }
}
