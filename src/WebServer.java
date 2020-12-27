import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A class for the WebServer.
 */
public class WebServer {
    private ServerSocket ss; // Listen for client connection requests on this server socket

    /**
     * Constructor for the WebServer class.
     * Creates a pool of threads and runs instances of ConnectionHandler in the thread pool.
     * @param path The document_root given in the command-line arguments.
     * @param port The port given in the command-line arguments.
     */
    public WebServer(String path, int port) {
        ExecutorService threadPool;
        try {
            ss = new ServerSocket(port);
            System.out.println("Server started ... listening on port " + port + " ...");
            threadPool = Executors.newFixedThreadPool(Configuration.THREAD_LIMIT);

            while (true) {
                // Waits until client requests a connection, then returns a connection (socket)
                Socket conn = ss.accept();
                System.out.println("Server got new connection request from " + conn.getInetAddress());

                // Runnable ConnectionHandler
                Runnable ch = new ConnectionHandler(conn, path);
                threadPool.execute(ch);
            }
        } catch (IOException ioe) {
            System.err.println("WebServer (error): " + ioe.getMessage());
        }
    }
}
