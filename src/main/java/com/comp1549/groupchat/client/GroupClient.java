package com.comp1549.groupchat.client;

import com.comp1549.groupchat.model.Member;
import com.comp1549.groupchat.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GroupClient {
    private static final Logger logger = LoggerFactory.getLogger(GroupClient.class);
    private static final int HEARTBEAT_INTERVAL = 15; // seconds

    private final String id;
    private final String serverHost;
    private final int serverPort;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running;
    private Member currentHost;

    public GroupClient(String id, String serverHost, int serverPort) throws IOException {
        this.id = id;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.socket = new Socket(serverHost, serverPort);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.running = true;
    }

    public void start() throws IOException {
        // Send join message
        Message joinMessage = new Message(id, null, Message.Type.JOIN, 
            String.format("Joining from %s:%d", socket.getLocalAddress().getHostAddress(), socket.getLocalPort()));
        out.writeObject(joinMessage);
        out.flush();

        // Start heartbeat sender
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 
            HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);

        // Start message receiver thread
        new Thread(this::receiveMessages).start();

        // Display commands and start command line interface
        displayCommands();
        startCommandLineInterface();
    }

    private void receiveMessages() {
        try {
            while (running) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                logger.error("Error receiving messages", e);
            }
        }
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case PRIVATE:
            case BROADCAST:
                System.out.printf("\nReceived from %s: %s%n", message.getSenderId(), message.getContent());
                break;
            case MEMBER_LIST:
                updateMemberList(message.getContent());
                break;
            case ERROR:
                System.out.printf("\nError: %s%n", message.getContent());
                if (message.getContent().contains("User ID already exists")) {
                    System.out.println("Exiting due to duplicate user ID...");
                    try {
                        quit();
                    } catch (IOException e) {
                        logger.error("Error while quitting", e);
                        System.exit(1);
                    }
                }
                break;
            default:
                logger.debug("Received message of type {}: {}", message.getType(), message.getContent());
        }
    }

    private void updateMemberList(String memberListStr) {
        System.out.println("\n=== Current Group Members ===");
        
        // Handle empty list case
        if (memberListStr == null || memberListStr.trim().equals("[]")) {
            System.out.println("No members connected");
            return;
        }

        // Remove brackets and split by "Member{"
        String[] members = memberListStr.substring(1, memberListStr.length() - 1).split("Member\\{");
        
        for (String member : members) {
            // Skip empty entries
            if (member.trim().isEmpty()) continue;
            
            try {
                // Extract values using regex patterns for safety
                String id = member.matches(".*id='([^']*)'.*") ? 
                    member.replaceAll(".*id='([^']*)'.*", "$1") : "unknown";
                    
                String ip = member.matches(".*ip=([^,]*).*") ?
                    member.replaceAll(".*ip=([^,]*).*", "$1") : "unknown";
                    
                String port = member.matches(".*port=(\\d+).*") ?
                    member.replaceAll(".*port=(\\d+).*", "$1") : "unknown";
                    
                boolean isHost = member.contains("host=true");

                // Format and print each member
                System.out.printf("%-15s %s:%-6s %s%n", 
                    id,
                    ip,
                    port,
                    isHost ? "[Host]" : ""
                );
            } catch (Exception e) {
                System.out.println("Error parsing member: " + member);
            }
        }
    }

    private void sendHeartbeat() {
        try {
            Message heartbeat = Message.createHeartbeat(id);
            out.writeObject(heartbeat);
            out.flush();
        } catch (IOException e) {
            logger.error("Error sending heartbeat", e);
        }
    }

    private void startCommandLineInterface() {
        Scanner scanner = new Scanner(System.in);

        while (running) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 3);
            try {
                switch (parts[0].toLowerCase()) {
                    case "broadcast":
                        if (parts.length < 2) {
                            System.out.println("Usage: broadcast <message>");
                            continue;
                        }
                        sendBroadcast(parts[1]);
                        break;
                    case "private":
                        if (parts.length < 3) {
                            System.out.println("Usage: private <recipient-id> <message>");
                            continue;
                        }
                        sendPrivate(parts[1], parts[2]);
                        break;
                    case "quit":
                        quit();
                        return;
                    default:
                        System.out.println("Unknown command. Type 'help' for available commands.");
                }
            } catch (IOException e) {
                logger.error("Error processing command", e);
            }
        }
    }

    private void displayCommands() {
        System.out.println("\nCommands:");
        System.out.println("1. broadcast <message> - Send message to all members");
        System.out.println("2. private <recipient-id> <message> - Send private message");
        System.out.println("3. quit - Leave the group");
    }

    private void sendBroadcast(String content) throws IOException {
        Message message = Message.createBroadcast(id, content);
        out.writeObject(message);
        out.flush();
    }

    private void sendPrivate(String recipientId, String content) throws IOException {
        Message message = Message.createPrivate(id, recipientId, content);
        out.writeObject(message);
        out.flush();
    }

    private void quit() throws IOException {
        running = false;
        Message leaveMessage = new Message(id, null, Message.Type.LEAVE, "Leaving group");
        out.writeObject(leaveMessage);
        out.flush();
        
        scheduler.shutdown();
        socket.close();
        System.exit(0);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GroupClient <client-id> [server-host] [server-port]");
            return;
        }

        String clientId = args[0];
        String serverHost = args.length > 1 ? args[1] : "localhost";
        int serverPort = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

        try {
            GroupClient client = new GroupClient(clientId, serverHost, serverPort);
            client.start();
        } catch (IOException e) {
            logger.error("Error starting client", e);
        }
    }
} 