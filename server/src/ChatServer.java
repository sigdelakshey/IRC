import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {

    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private static final int DEFAULT_PORT = 6667;
    private static final int DEFAULT_DEBUG_LEVEL = 0;
    private static final int THREAD_LIMIT = 4;

    private final int port;
    private final int debugLevel;
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final ServerManager serverManager;

    public ChatServer(int port, int debugLevel) throws IOException {
        this.port = port;
        this.debugLevel = debugLevel;
        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newFixedThreadPool(THREAD_LIMIT);
        this.serverManager = new ServerManager();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public void start() throws IOException {
        logger.log(Level.INFO, "Chat server started on port {0}", port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            logger.log(Level.INFO, "Client connected: {0}", clientSocket.getRemoteSocketAddress());
            ClientHandler clientHandler = new ClientHandler(clientSocket, serverManager, debugLevel);
            threadPool.submit(clientHandler);
        }
    }

    private void shutdown() throws IOException {
        logger.log(Level.INFO, "Shutting down server...");
        serverManager.broadcastMessage("Server shutting down.");
        serverManager.closeAllConnections();
        threadPool.shutdown();
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing server socket", e);
        }
        logger.log(Level.INFO, "Server stopped.");
    }

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        int debugLevel = DEFAULT_DEBUG_LEVEL;

        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-p")) {
                    port = Integer.parseInt(args[i + 1]);
                } else if (args[i].equals("-d")) {
                    debugLevel = Integer.parseInt(args[i + 1]);
                }
            }
        }

        ChatServer server = new ChatServer(port, debugLevel);
        server.start();
    }
}