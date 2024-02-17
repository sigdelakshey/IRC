import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final Socket clientSocket;
    private final ServerManager serverManager;
    private final int debugLevel;
    private String nickname;
    private final Set<Channel> channels;

    private BufferedReader reader;
    private PrintWriter writer;

    public ClientHandler(Socket clientSocket, ServerManager serverManager, int debugLevel) throws IOException {
        this.clientSocket = clientSocket;
        this.serverManager = serverManager;
        this.debugLevel = debugLevel;
        this.channels = new HashSet<>();
        this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.writer = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            handleHandshake();
            processCommands();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error communicating with client", e);
        } finally {
            closeConnection();
        }
    }

    private void handleHandshake() throws IOException {
        writer.println("Welcome to the chat server!");
        while (true) {
            String command = reader.readLine();
            if (command.startsWith("/nick ")) {
                String newNickname = command.substring(6).trim();
                if (serverManager.addUser(this, newNickname)) {
                    nickname = newNickname;
                    writer.println("Nickname set to: " + nickname);
                    break;
                } else {
                    writer.println("Nickname already taken. Please choose another.");
                }
            } else {
                writer.println("Invalid command. Please use /nick <nickname> to set your nickname.");
            }
        }
    }

    private void processCommands() throws IOException {
        while (true) {
            String input = reader.readLine();
            String[] parts = input.split(" ", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1] : "";
            if (command == null) {
                break;
            }

            if (debugLevel > 0) {
                logger.log(Level.INFO, "Client " + nickname + ": " + command);
            }

            switch (command) {
                case "/list":
                    listChannels();
                    break;
                case "/join":
                    joinChannel(argument);
                    break;
                case "/leave":
                    leaveChannel(argument);
                    break;
                case "/quit":
                    disconnect();
                    break;
                case "/help":
                    sendHelpMessage();
                    break;
                case "/stats":
                    sendStats();
                    break;
                case "/cmsg":
                    sendChannelMessage(argument);
                    break;
                case "/pmsg":
                    sendPrivateMessage(argument);
                    break;
                default:
                    sendMessageToAllChannels(command);
                    break;
            }
        }
    }

    private void listChannels() throws IOException {
        List<String> channelList = serverManager.getChannelList();
        writer.println("Available channels:");
        for (String channel : channelList) {
            writer.println("- " + channel);
        }
    }

    private void joinChannel(String channelName) throws IOException {
        serverManager.joinChannel(this, channelName);
        writer.println("Joined channel: " + channelName);
    }

    private void leaveChannel(String channelName) throws IOException {
        serverManager.leaveChannel(this, channelName);
        channels.remove(serverManager.getChannel(channelName));
        writer.println("Left channel: " + channelName);
    }

    public void disconnect() throws IOException {
        serverManager.removeUser(this);
        writer.println("Goodbye!");
        writer.flush();
    }

    private void sendHelpMessage() throws IOException {
        writer.println("Available commands:");
        writer.println("/list: List available channels");
        writer.println("/join <channel>: Join a channel");
        writer.println("/leave <channel>: Leave a channel");
        writer.println("/quit: Disconnect from the server");
        writer.println("/help: Display this help message");
        writer.println("/stats: Show server statistics");
        writer.println("/cmsg <userNickName> <message>: Send a private message to another user");
        writer.println("/pmsg <channelName> <message>: Send a private message to a channel");
        writer.println("<message>: Send message to all active channels");
        }

    private void sendMessageToAllChannels(String message) throws IOException {
        for (Channel channel : channels) {
            channel.broadcastMessage(String.format("[%s] %s", nickname, message));
        }
    }

    public void sendPrivateMessage(String message) throws IOException {
        String[] parts = message.split(" ", 2);
        String recipient = parts[0];
        String privateMessage = parts.length > 1 ? parts[1] : "";
        serverManager.sendPrivateMessage(this, recipient, privateMessage);
    }

    public void sendChannelMessage(String message) throws IOException {
            String[] parts = message.split(" ", 2);
            String recipient = parts[0];
            String privateMessage = parts.length > 1 ? parts[1] : "";
            serverManager.sendChannelMessage(this, recipient, privateMessage);
    }

    private void sendStats() throws IOException {
        Map<String, Integer> userStats = serverManager.getUserStats();
        Integer count = serverManager.getTotalUniqueUsers();
        writer.println(String.format("Number of users: %s", count));
        writer.println("Server statistics:");
        for (Map.Entry<String, Integer> entry : userStats.entrySet()) {
            writer.println(String.format("Channel %s: %d users", entry.getKey(), entry.getValue()));
        }
    }

    private void closeConnection() {
        try {
            reader.close();
            writer.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing client connection", e);
        }
    }

    public String getNickname() {
        return nickname;
    }

    public Set<Channel> getChannels() {
        return channels;
    }

    public void addChannel(Channel channel) {
        channels.add(channel);
    }

    public void removeChannel(Channel channel) {
        channels.remove(channel);
    }

    public void sendMessageUser(String message) {
        writer.println(message);
    }
}  