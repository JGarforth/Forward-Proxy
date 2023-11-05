/**
 * Various methods to verify the parsed data is valid for a connection.
 * */

public class DataVerification {

    public static boolean isParseValid (ClientHandler.ParsedData parsedData) {
        return isValidPort(parsedData.port()) && isValidHost(parsedData.host());
    }

    private static boolean isValidPort (int port) {
        return port >= 1 && port <= 65535;
    }

    private static boolean isValidHost (String host) {
        return host != null && !host.isEmpty() && !isLocalHost(host);
    }

    private static boolean isLocalHost (String host) {
        String[] localConnections = {"localhost", "127.0.0.1", "::1",
                "10.", "192.168.", "172.16.", "172.17.", "172.18.", "172.19.",
                "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
                "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.", "fd00:/8"};

        for (String connection : localConnections) {
            if (host.toLowerCase().startsWith(connection)) return true;
        }

        return false;
    }


}
