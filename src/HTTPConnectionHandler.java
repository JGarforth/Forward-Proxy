/**
 * Handles HTTP connections. Sends a stream to the target, which then responds.
 * Response stream is then forwarded to the client.
 * Method allows for the modification of the HTTP header to conceal the originating IP.
 * WARNING: If get IP fails, IP will not be concealed.
 * */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HTTPConnectionHandler extends ConnectionHandler {

    public HTTPConnectionHandler(Socket clientSocket, ConfigurationManager config) {
        super(clientSocket, config);
    }

    @Override
    public void handleConnection(String fullRequest, OutputStream clientOutput, ClientHandler.ParsedData parsedData) {
        try {
            if (config.getConfig("MaskIP").equals("1")) {
                fullRequest = concealUserIP(fullRequest);
            }
            try (Socket targetSocket = new Socket(parsedData.host(), parsedData.port())) {

                OutputStream targetOutputStream = targetSocket.getOutputStream();
                InputStream targetInputStream = targetSocket.getInputStream();

                targetOutputStream.write(fullRequest.getBytes(StandardCharsets.UTF_8));
                targetOutputStream.flush();

                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                byte[] responseBuffer = new byte[1024];
                int responseBytesRead;
                while ((responseBytesRead = targetInputStream.read(responseBuffer)) != -1) {
                    responseStream.write(responseBuffer, 0, responseBytesRead);
                }

                clientOutput.write(responseStream.toByteArray());
                clientOutput.flush();
            }
        } catch (IOException e) {
            Logger.logError("Connection to target failed: " + e.getMessage());
        }
    }
}
