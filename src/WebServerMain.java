/**
 * A class for the main method.
 */
public class WebServerMain {

    /**
     * Creates a new WebServer object.
     * Checks whether command-line arguments are supplied and if yes, parses them.
     * If not, exits with usage error by calling exitWithUsageError.
     * @param args document_root and port, which are parsed and passed to the WebServer object.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            exitWithUsageError();
        }
        String path = args[0];
        int port = Integer.parseInt(args[1]);

        WebServer ws = new WebServer(path, port);
    }

    private static void exitWithUsageError() {
        System.out.println("Usage: java WebServerMain <document_root> <port>");
        System.exit(1);
    }
}
