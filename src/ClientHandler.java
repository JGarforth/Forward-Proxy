/**
 * Clienthandler generates the connections for the client.
 * Calls connection handlers to manage the connections and data output/input
 * Returns errors to the client for malformed or otherwise invalid requests
 * */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final ConfigurationManager config;
    private final Set<String> activeConnections;

    public ClientHandler(Socket clientSocket, ConfigurationManager config, Set<String> activeConnections) {
        this.clientSocket = clientSocket;
        this.config = config;
        this.activeConnections = activeConnections;
    }

    @Override
    public void run() {
        try (InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream()) {
            while (!clientSocket.isClosed()) {
                String fullRequest = readFullRequest(input);
                Logger.logInfo("Request received: " + fullRequest);

                // Checking for HTTPS first
                String[] requestParts = fullRequest.split(" ");
                if (requestParts.length >= 3 && requestParts[0].equalsIgnoreCase("connect")) {
                    String[] hostPort = requestParts[1].split(":");
                    String host = hostPort[0];
                    int port = Integer.parseInt(hostPort[1]);

                    Logger.logInfo("Handling HTTPS request");
                    HTTPSConnectionHandler httpsConnectionHandler = new HTTPSConnectionHandler(clientSocket, config);
                    httpsConnectionHandler.establishTunnel(host, port);
                }
                else {
                    ParsedData parsedData = parseRequest(fullRequest, output);
                    // Invalid HTTP request
                    if (parsedData == null || !DataVerification.isParseValid(parsedData)) {
                        sendBadRequestResponse(output);
                        return;
                    }
                    // Now checking for HTTP
                    else if (parsedData.port() == 80) {
                        Logger.logInfo("Handling HTTP request for host: " + parsedData.host());
                        HTTPConnectionHandler httpConnectionHandler = new HTTPConnectionHandler(clientSocket, config);
                        httpConnectionHandler.handleConnection(fullRequest, output, parsedData);
                    } else {
                        sendBadRequestResponse(output);
                        Logger.logError("Unsupported port: " + parsedData.port() + " from: " +
                                clientSocket.getInetAddress().getHostAddress());
                    }
                }
            }
        }
        catch (IOException e) {
            Logger.logError("I/O error with client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
        }
        finally {
            activeConnections.remove(clientSocket.getInetAddress().getHostAddress());
        }

    }

    private ParsedData parseRequest(String request, OutputStream output) {
        String[] parts = request.split(" ");

        if (parts.length < 3) {
            Logger.logError("Malformed request from: " + clientSocket.getInetAddress().getHostAddress());
            sendBadRequestResponse(output);
            return null;
        }

        try {
            URI uri = new URI(parts[1]);
            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 80;

            return new ParsedData(port, host);
        } catch (URISyntaxException e) {
            Logger.logError("URL syntax error from client " + clientSocket.getInetAddress().getHostAddress() + ": " +
                    e.getMessage());
           sendBadRequestResponse(output);
            return null;
        }
    }

    private void sendBadRequestResponse(OutputStream output) {
        String responseBody = "Bad Request: The request could not be parsed.";
        String response = "HTTP/1.1 400 Bad Request\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + responseBody.length() + "\r\n" +
                "\r\n" +
                responseBody;
        try {
            output.write(response.getBytes());
            output.flush();
        } catch (IOException e) {
            Logger.logError("Failed to send 400 response: " + e.getMessage());
        }
    }

    private String readFullRequest(InputStream input) {
        // Dynamically allocates buffer.
        // \r\n\r\n denotes the end of a http request
        ByteArrayOutputStream requestStream = new ByteArrayOutputStream();
        int length;
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                if ((length = input.read(buffer)) != -1) {
                    requestStream.write(buffer, 0, length);
                    if (requestStream.toString(StandardCharsets.UTF_8).contains("\r\n\r\n")) {
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return requestStream.toString(StandardCharsets.UTF_8);
    }

    public record ParsedData(int port, String host) {}
}
