import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles client requests.
 */
public class ConnectionHandler implements Runnable {
    private Socket conn;
    private InputStream is;
    private OutputStream os;
    private BufferedReader br; // Use buffered reader to read client data
    private String path;
    private BufferedWriter log;
    private LocalDateTime ldt;
    private DateTimeFormatter dtf;
    private String fldt;

    /**
     * Constructor for the ConnectionHandler class.
     * @param conn Socket representing the TCP/IP connection to the client.
     * @param path The document_root given in the command-line arguments.
     */
    public ConnectionHandler(Socket conn, String path) {
        this.conn = conn;
        this.path = path;
        this.ldt = LocalDateTime.now();
        this.dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.fldt = ldt.format(dtf);

        try {
            is = conn.getInputStream(); // Get data from client on this input stream
            os = conn.getOutputStream(); // Send data back to the client on this stream
            br = new BufferedReader(new InputStreamReader(is)); // Use buffered reader to read data
            // Open (and possibly create) a log file in specified directory according to Configuration.LOG_FILEPATH
            log = new BufferedWriter(new FileWriter(Configuration.LOG_FILEPATH, true));
        } catch (IOException ioe) {
            System.err.println("ConnectionHandler:constructor (error): " + ioe.getMessage());
        }
    }

    /**
     * Calls handleClientRequest.
     * Automatically called when an instance of ConnectionHandler, which implements the Runnable interface, is created.
     */
    @Override
    public void run() {
        handleClientRequest();
    }

    /**
     * Handles client connection requests by interpreting them, responding accordingly and logging requests
     * as well as response codes.
     */
    public void handleClientRequest() {
        System.out.println("... new ConnectionHandler constructed ...");
        try {
            while (true) {
                // Get input data from client over socket
                String line = br.readLine();
                String[] lineArray = line.split(" ");
                String requestType = lineArray[0];
                String resourceName = lineArray[1];
                // Log this request
                log.append("\nRequest at ").append(fldt).append(":\n").append(line);
                log.newLine();
                File f = new File(path + resourceName);
                String contentLength = String.valueOf(f.length());

                // Interpret request and respond accordingly
                if (!f.isFile()) {
                    byte[] response = getHeader(404, contentLength, resourceName);

                    os.write(response);
                    // Log this response
                    log.append("Response code:\n404 Not Found");
                    log.newLine();
                    break;
                } else if (requestType.contains("GET")) {
                    byte[] header = getHeader(200, contentLength, resourceName);
                    byte[] content = getContent(f);
                    byte[] response = new byte[header.length + content.length];
                    System.arraycopy(header, 0, response, 0, header.length);
                    System.arraycopy(content, 0, response, header.length, content.length);

                    os.write(response);
                    // Log this response
                    log.append("Response code:\n200 OK");
                    log.newLine();
                    break;
                } else if (requestType.equals("HEAD")) {
                    byte[] response = getHeader(200, contentLength, resourceName);

                    os.write(response);
                    // Log this response
                    log.append("Response code:\n200 OK");
                    log.newLine();
                    break;
                } else if (requestType.equals("DELETE")) {
                    f.delete();
                    break;
                } else {
                    byte[] response = getHeader(501, contentLength, resourceName);

                    os.write(response);
                    // Log this response
                    log.append("Response code:\n501 Not Implemented");
                    log.newLine();
                    break;
                }
            }
            cleanUp();
        } catch (IOException | IndexOutOfBoundsException e) {
            System.err.println("ConnectionHandler:handleClientRequest (error): " + e.getMessage());
            cleanUp();
        }
    }

    /**
     * Builds an appropriately formatted header depending on whether the request method is supported and
     * whether the resource can be found.
     * @param responseCode Corresponding HTTP response code.
     * @param contentLength Length of the resource.
     * @param resourceName Name of the resource, trailing the '/' in the path.
     * @return Appropriately formatted header.
     */
    private byte[] getHeader(int responseCode, String contentLength, String resourceName) {
        String specificResponse = "";
        switch (responseCode) {
            case 200:
                specificResponse = responseCode + " " + "OK";
                break;
            case 404:
                specificResponse = responseCode + " " + "Not Found";
                break;
            case 501:
                specificResponse = responseCode + " " + "Not Implemented";
                break;
        }

        // Build header
        String header = "HTTP/1.1 " + specificResponse + "\r\n";
        header += "Server: " + Configuration.SERVER_NAME + "\r\n";
        if (resourceName.contains(".gif") || resourceName.contains(".jpg") || resourceName.contains(".png")) {
            header += "Content-Type: image/jpeg\r\n";
        } else {
            header += "Content-Type: text/html\r\n";
        }
        header += "Content-Length: " + contentLength + "\r\n\r\n";

        return header.getBytes();
    }


    /**
     * Reads in content from the specified resource (a file).
     * @param f The specified resource.
     * @return The content.
     */
    private byte[] getContent(File f) {
        byte[] content = null;

        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] temp = new byte[fis.available()];
            fis.read(temp);
            fis.close();
            content = temp;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        return content;
    }

    /**
     * Closes the instances of BufferedReader, InputStream and Socket as well BufferedWriter,
     * which is used for logging.
     */
    private void cleanUp() {
        System.out.println("ConnectionHandler: ... cleaning up and exiting ...");
        try {
            br.close();
            is.close();
            conn.close();
            log.close();
        } catch (IOException ioe) {
            System.err.println("ConnectionHandler:cleanup: " + ioe.getMessage());
        }
    }
}
