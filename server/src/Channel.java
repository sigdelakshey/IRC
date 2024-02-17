import java.io.IOException;
//import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Channel {

    private final String name;
    private final Set<ClientHandler> users;

    public Channel(String name) {
        this.name = name;
        this.users = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void addUser(ClientHandler client) {
        users.add(client);
    }

    public void removeUser(ClientHandler client) {
        users.remove(client);
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    public int getUserCount() {
        return users.size();
    }

    public List<String> getUserNicknames() {
        return users.stream().map(ClientHandler::getNickname).collect(Collectors.toList());
    }

    public void broadcastMessage(String message) throws IOException {
        for (ClientHandler client : users) {
            client.sendMessageUser(message);
        }
    }
}