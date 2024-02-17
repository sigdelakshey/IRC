import java.io.IOException;

public class CommandHandler {

    private final ChatClient client;

    public CommandHandler(ChatClient client) {
        this.client = client;
    }

    public void handleCommand(String input) throws IOException {
        String[] parts = input.split(" ", 2);
        String command = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "/connect":
                handleConnect(argument);
                break;
            case "/nick":
                handleNick(argument);
                break;
            case "/list":
                client.sendMessage("/list");
                break;
            case "/join":
                client.sendMessage("/join" + " " + argument);
                break;
            case "/leave":
                client.sendMessage("/leave" + " " + argument);
                break;
            case "/quit":
                client.sendMessage("/quit");
                client.disconnect();
                break;
            case "/help":
                client.sendMessage("/help");
                break;
            case "/stats":
                client.sendMessage("/stats");
                break;
            default:
                client.sendMessage(input);
        }
    }

    private void handleConnect(String argument) throws IOException {
        String[] parts = argument.split(" ");
        if (parts.length != 2) {
            System.out.println("Invalid format. Use /connect <server-name> <port-number>");
            return;
        }
        String serverName = parts[0];
        int portNumber = Integer.parseInt(parts[1]);
        //client.disconnect(); // Close existing connection (if any)
        client.connect(serverName, portNumber);
        System.out.println("Connected to server!");
    }

    private void handleNick(String argument) throws IOException {
        client.sendMessage("/nick " + argument);
    }

}