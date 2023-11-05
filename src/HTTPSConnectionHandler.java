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
        Thread clientToServer = new Thread(() -> {
            try {
                InputStream clientInput = clientSocket.getInputStream();
                OutputStream serverOutput = serverSocket.getOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = clientInput.read(buffer)) != -1) {
                    serverOutput.write(buffer, 0, read);
                    serverOutput.flush();
                }
            } catch (IOException e) {
                Logger.logError("Could not pass data from client to target: " + e);
            }
        });

        Thread serverToClient = new Thread(() -> {
            try {
                InputStream serverInput = serverSocket.getInputStream();
                OutputStream clientOutput = clientSocket.getOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = serverInput.read(buffer)) != -1) {
                    clientOutput.write(buffer, 0, read);
                    clientOutput.flush();
                }
            } catch (IOException e) {
                Logger.logError("Could not retrieve data from the target: " + e);
            }
        });

        clientToServer.start();
        serverToClient.start();
    }

    private Socket initializeSSLToTarget(String targetHost, int targetPort) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        return factory.createSocket(targetHost, targetPort);
    }
}
