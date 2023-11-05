/**
 * Abstract class used to define all connection handlers.
 * Handle connection is to be modified by all children to handle various connection types.
 * Finds server IP and uses that in headers to conceal user IP.
 * Method modifies the header of the request to hold the new IP.
 * NOTE: NOT USED BY HTTPS. HTTPS is encrypted, can't read/modify.
 * */
import java.io.OutputStream;
import java.net.Socket;

public abstract class ConnectionHandler {

    protected final Socket clientSocket;
    protected ConfigurationManager config;

    public ConnectionHandler(Socket clientSocket, ConfigurationManager config) {

        this.clientSocket = clientSocket;
        this.config = config;
    }

    protected abstract void handleConnection(String fullRequest, OutputStream clientOutput, ClientHandler.ParsedData parsedData);

    protected String concealUserIP(String fullRequest) {

        String[] lines = fullRequest.split("\r\n");
        StringBuilder modifiedRequest = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("X-Forwarded-For:") || line.startsWith("X-Real-IP:")) {
                continue;
            }
            modifiedRequest.append(line).append("\r\n");
        }

        modifiedRequest.append("X-Forwarded-For: 0.0.0.0\r\n");

        modifiedRequest.append("\r\n").append(fullRequest.substring(fullRequest.indexOf("\r\n\r\n") + 4));
        return modifiedRequest.toString();
    }


}
