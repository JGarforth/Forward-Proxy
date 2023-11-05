/**
 * Reads ServerConfig and returns the settings. Allows for default log location of desktop.
 * loadConfig is called on instantiation to fill map with data.
 * When changes are made to the settings with setConfig, they are saved to the file with
 * saveconfig.
 * getConfig returns the setting values.
 * */

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationManager {

    private static ConfigurationManager instance;
    private final Map<String, String> configMap = new HashMap<>();
    private final File configFilePath;

    private ConfigurationManager(String configFile) throws Exception{
        URL path = ClassLoader.getSystemResource(configFile);
        if (path == null) {
            throw new Exception("Cannot find config file.");
        }
        this.configFilePath = new File(path.toURI());
        loadConfig();
    }

    public static ConfigurationManager getInstance(String configFilePath) throws Exception{
        if (instance == null) {
            try {
                instance = new ConfigurationManager(configFilePath);
            } catch (Exception e) {
                throw new Exception(e);
            }
        }
        return instance;
    }

    private void loadConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processConfigLine(line);
            }
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
    }

    public String getConfig(String key) {
        return configMap.get(key);
    }

    public void setConfig(String key, String value) {
        configMap.put(key, value);
        saveConfig();
    }

    private void saveConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFilePath))) {
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    private void processConfigLine(String line) {
        String[] parts = line.split(": ");
        if (parts.length != 2) {
            System.err.println("Skipping invalid config line: " + line);
            return;
        }
        configMap.put(parts[0].trim(), resolvePlaceholders(parts[0].trim(), parts[1].trim()));
    }

    private String resolvePlaceholders(String key, String value) {
        if ("LogFilePath".equals(key) && value.contains("%DESKTOP%")) {
            String desktopPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator;
            return value.replace("%DESKTOP%", desktopPath);
        }
        return value;
    }

    public static boolean generateDefaultConfigFile() {
        String fileName = "ServerConfig";
        String defaultContent = "ServerPort: 0\nLogFilePath: %DESKTOP%/server_log.txt\nMaskIP: 0";

        File configFile = new File(System.getProperty("user.dir") + "\\src", fileName);
        System.out.println("Generating default config file at: " + configFile.getAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
            writer.write(defaultContent);
        } catch (IOException e) {
            System.err.println("Failed to create default config file: " + e.getMessage());
            return false;
        }
        return true;
    }
}
