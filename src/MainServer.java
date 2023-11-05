/**
 * Initializes server socket and then attempts to start a client thread.
 * Currently, closes completely with any errors.
 * Implements runnable so that the server CLI can still be interacted with.
 * */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer implements Runnable{
    private final ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private final int port;
    private final ConfigurationManager config;
    private Thread serverThread;

    public MainServer(int port, ConfigurationManager config) {
        this.port = port;
        this.config = config;
        try {
            this.serverSocket = new ServerSocket(this.port);
            Logger.logInfo("Server socket created and started listening on port: " + this.port); // Detailed log
        } catch (IOException e) {
            System.err.println("Error: Server failed to start on port: " + this.port); // CLI output for user
            Logger.logError("Server failed. Could not listen on port: " + this.port + ". Error: " + e.getMessage()); // Detailed log
            throw new RuntimeException("Could not start server on port: " + this.port, e);
        }
    }

    public void startServer() {
        if (isRunning) {
            System.err.println("Server is already running.");
            return;
        }

        isRunning = true;
        serverThread = new Thread(this);
        serverThread.start();
    }

    @Override
    public void run() {
        System.out.println("Server is now running on port: " + port);
        Logger.logInfo("Server execution started on port: " + port);

        while (isRunning && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());
                Logger.logInfo("New client connection accepted from: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler newClient = new ClientHandler(clientSocket, config);
                new Thread(newClient).start();
            } catch (IOException e) {
                if (isRunning) {
                    Logger.logError("Unexpected IO exception during accept: " + e.getMessage());
                }
            }
        }
        Logger.logInfo("Server has stopped accepting new connections.");
    }

    public void shutdown() {
        isRunning = false;
        try {
            serverSocket.close();
            serverThread.join();
            System.out.println("Server has been stopped.");
            Logger.logInfo("Server socket closed. Server shutdown successfully.");
        } catch (IOException e) {
            System.err.println("Failed to close server socket.");
            Logger.logError("Error closing server socket: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Server shutdown was interrupted.");
            Logger.logError("Server shutdown interrupted: " + e.getMessage());
        }
    }
}