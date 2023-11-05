/**
 * Main. Starts cli menu to initialize server.
 * start [port] starts the server.
 * Settings may be applied in the form of '-[setting]'.
 * stop stops the server.
 * */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {

    private static MainServer server = null;
    private static ConfigurationManager config;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        if (!initializeConfigManager(scanner)) return;
        Logger.initializeLogger(config.getConfig("LogFilePath"));

        System.out.print("> ");

        boolean run = true;
        while (run) {
            String userInput = scanner.nextLine().trim();
            if (userInput.isEmpty()) {
                continue;
            }

            String[] parts = userInput.split("\\s+");

            for (int i = 0; i < parts.length; i++) {
                switch (parts[i]) {
                    case "start" -> {
                        i++;
                        String portNumber = parts[i];
                        handleStartCommand(portNumber);
                    }
                    case "stop" -> {
                        handleStopCommand();
                        run = false;
                    }
                    case "-h" -> System.out.println(help);
                    case "-m" -> {
                        if (config.getConfig("MaskIP").equals("0")) {
                            config.setConfig("MaskIP", "1");
                            System.out.println("Concealment enabled.");
                        } else {
                            config.setConfig("MaskIP", "0");
                            System.out.println("Concealment disabled.");
                        }
                    }
                    default -> System.out.println("Unknown command. Try '-h' for help.");
                }
            }
            System.out.print("> ");
        }
    }

    private static void handleStartCommand(String portNumber) {
        if (portNumber == null) {
            System.out.println("Port number is required to start the server.");
            return;
        }
        try {
            int port = Integer.parseInt(portNumber.trim());
            server = new MainServer(port, config);
            server.startServer();
            config.setConfig("ServerPort", String.valueOf(port));
            System.out.println("Server started: Listening on " + port);
            try {
                InetAddress thisMachine = InetAddress.getLocalHost();
                System.out.println("Proxy IP Address: " + thisMachine.getHostAddress());
            } catch (UnknownHostException e) {
                System.out.println("WARNING: Could not access machine IP.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Port is required to start the server.");
        }
    }

    private static void handleStopCommand() {
        if (server != null) {
            server.shutdown();
            System.out.println("Server stopped.");
        } else {
            System.out.println("Server is not running.");
        }
    }

    private static boolean initializeConfigManager(Scanner scanner) {
        try {
            config = ConfigurationManager.getInstance("ServerConfig");
        } catch (Exception e) {
            String error = e.toString();
            System.out.println(error);
            boolean mustGen = true;
            while (mustGen) {
                System.out.print("Generate new config and try again? (y/n)\n> ");
                String selection = scanner.nextLine().trim();
                switch (selection.toLowerCase()) {
                    case "y" -> {
                        if (!ConfigurationManager.generateDefaultConfigFile()) return false;
                        initializeConfigManager(scanner);
                        mustGen = false;
                    }
                    case "n" -> {
                        return false;
                    }
                    default -> System.out.print("Invalid input. Please input y or n.");
                }
            }
        }
        return true;
    }

    private static final String help = """
            ALL COMMANDS:
            start [port] : start server with specified port
            stop : stop the server
            -h : display this help message
            -m : conceal IP""";
}
