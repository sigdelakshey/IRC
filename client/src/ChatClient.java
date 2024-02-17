import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatClient {

    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String nickname = "Guest";
    private CommandHandler commandHandler;

    public ChatClient() {
        commandHandler = new CommandHandler(this);
    }

    public void connect(String serverName, int portNumber) throws IOException {
        socket = new Socket(serverName, portNumber);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        Thread readerThread = new Thread(this::listenForMessages);
        readerThread.start();
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error reading from server", e);
        }
    }

    public void handleInput() {
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String input;
            while ((input = consoleReader.readLine()) != null) {
                commandHandler.handleCommand(input);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error reading from console", e);
        }
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing socket", e);
        }
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient();
        // No default connection initially 
        //client.connect("localhost", 1009); 
        client.handleInput();
    }
}