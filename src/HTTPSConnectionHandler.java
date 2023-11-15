/**
 * HTTPSConnectionHandler initiates a tunnel with the target, and
 * utilizes two threads to pass the packets back and forth.
 * */
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HTTPSConnectionHandler {

    private final Socket clientSocket;
    private final ConfigurationManager config;

    public HTTPSConnectionHandler(Socket clientSocket, ConfigurationManager config) {
        this.clientSocket = clientSocket;
        this.config = config;
    }

    public void establishTunnel(String targetHost, int targetPort) throws IOException {
        try (Socket serverSocket = initializeSSLToTarget(targetHost, targetPort)) {
            sendTunnelEstablishedResponse(clientSocket.getOutputStream());

            relayTraffic(clientSocket, serverSocket);
        } catch (IOException e) {
            Logger.logError("Error establishing HTTPS tunnel: " + e.getMessage());
            throw e;
        }
    }

    private void sendTunnelEstablishedResponse(OutputStream clientOutput) throws IOException {
        String response = "HTTP/1.1 200 Connection Established\r\n\r\n";
        clientOutput.write(response.getBytes(StandardCharsets.UTF_8));
        clientOutput.flush();
    }

    private void relayTraffic(Socket clientSocket, Socket serverSocket) {
        try {
            Thread clientToServer = generateRelayThread(clientSocket.getInputStream(), serverSocket.getOutputStream());
            Thread serverToClient = generateRelayThread(serverSocket.getInputStream(), clientSocket.getOutputStream());

            clientToServer.start();
            serverToClient.start();

            clientToServer.join();
            serverToClient.join();
        } catch (InterruptedException e) {
            Logger.logError("Thread interrupted: " + e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            Logger.logError("Error in input/output stream : " + e);
        } finally {
            closeQuietly(serverSocket);
            closeQuietly(clientSocket);
        }
    }

    private Thread generateRelayThread(InputStream input, OutputStream output) {
        return  new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    output.flush();
                }
            } catch (IOException e) {
                Logger.logError("Could not pass data from client to target: " + e);
            }
        });
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                Logger.logError("Socket could not be closed : " + e);
            }
        }
    }

    private Socket initializeSSLToTarget(String targetHost, int targetPort) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        return factory.createSocket(targetHost, targetPort);
    }
}
