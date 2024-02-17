import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.logging.Level;
//import java.util.logging.Logger;

public class ServerManager {

    //private static final Logger logger = Logger.getLogger(ServerManager.class.getName());

    private final Map<String, Channel> channels;
    private final Map<String, ClientHandler> clients;
    private final Map<String, User> users;

    public ServerManager() {
        this.channels = new ConcurrentHashMap<>();
        this.clients = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
    }

    public synchronized boolean addUser(ClientHandler client, String nickname) throws IOException {
        if (users.containsKey(nickname)) {
            return false; 
        }
        User user = new User(nickname);
        users.put(nickname, user);
        clients.put(nickname, client);
        broadcastMessage(String.format("[[[Server]]]--> %s joined the chat.", nickname));
        return true;
    }

    public synchronized void removeUser(ClientHandler client) throws IOException {
        String nickname = client.getNickname();
        users.remove(nickname);
        clients.remove(nickname);
        for (Channel channel : client.getChannels()) {
            channel.removeUser(client);
            if (channel.isEmpty()) {
                channels.remove(channel.getName());
            } else {
                channel.broadcastMessage(String.format("[Server] %s left %s.", nickname, channel.getName()));
            }
        }
    }

    public synchronized void joinChannel(ClientHandler client, String channelName) throws IOException {
        Channel channel = channels.get(channelName);
        if (channel == null) {
            channel = new Channel(channelName);
            channels.put(channelName, channel);
        }
        channel.addUser(client);
        client.addChannel(channel);
        channel.broadcastMessage(String.format("[[[Server]]]--> %s joined %s.", client.getNickname(), channelName));
    }

    public synchronized void leaveChannel(ClientHandler client, String channelName) throws IOException {
        Channel channel = channels.get(channelName);
        if (channel != null) {
            channel.removeUser(client);
            client.removeChannel(channel);
            channel.broadcastMessage(String.format("[[[Server]]]--> %s left %s.", client.getNickname(), channelName));
        }
    }

    public synchronized void broadcastMessage(String message) throws IOException {
        for (Channel channel : channels.values()) {
            channel.broadcastMessage(message);
        }
    }

    public synchronized Message createMessage(String sender, String content) {
        return new Message(sender, content);
    }

    public synchronized void sendChannelMessage(ClientHandler sender, String recipient, String message) throws IOException {
        Channel receiver_channel = channels.get(recipient);
        if (receiver_channel != null) {
            receiver_channel.broadcastMessage(String.format("[[%s]] [%s] %s", receiver_channel.getName(),sender.getNickname(), message));
        } else {
            sender.sendMessageUser(String.format("[[[Server]]]--> User %s not found.", receiver_channel));
        }
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String recipient, String message) throws IOException {
        ClientHandler receiver = clients.get(recipient);
        if (receiver != null) {
            receiver.sendMessageUser(String.format("[%s] %s", sender.getNickname(), message));
        } else {
            sender.sendMessageUser(String.format("[[[Server]]]--> User %s not found.", recipient));
        }
    }

    public synchronized List<String> getChannelList() {
        return new ArrayList<>(channels.keySet());
    }

    public synchronized List<String> getUserList(String channelName) {
        Channel channel = channels.get(channelName);
        if (channel != null) {
            return channel.getUserNicknames();
        } else {
            return Collections.emptyList();
        }
    }

    public synchronized int getTotalUniqueUsers() {
        Set<String> uniqueUsers = new HashSet<>();
        for (Channel channel : channels.values()) {
            uniqueUsers.addAll(channel.getUserNicknames());
        }
        return uniqueUsers.size();
    }
    

    public synchronized Map<String, Integer> getUserStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (Channel channel : channels.values()) {
            stats.put(channel.getName(), channel.getUserCount());
        }
        return stats;
    }

    public synchronized void closeAllConnections() {
        for (ClientHandler client : clients.values()) {
            try {
                client.disconnect();
            } catch (IOException e) {
                System.err.println("Error disconnecting client: " + e.getMessage());
            }
        }
    }

    public synchronized Channel getChannel(String channelName) {
        return channels.get(channelName);
    }

    //private void log(Level level, String message) {
    //    if (logger.isLoggable(level)) {
    //        logger.log(level, message);
    //    }
    //}
}