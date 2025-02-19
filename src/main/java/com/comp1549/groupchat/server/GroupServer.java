package com.comp1549.groupchat.server;

import com.comp1549.groupchat.model.Member;
import com.comp1549.groupchat.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class GroupServer {
    private static final Logger logger = LoggerFactory.getLogger(GroupServer.class);
    private static final int HEARTBEAT_INTERVAL = 20; // seconds
    private static final int HEARTBEAT_TIMEOUT = 30; // seconds

    private final int port;
    private final ServerSocket serverSocket;
    private final Map<String, Member> members;
    private final Map<String, ObjectOutputStream> clientStreams;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running;

    public GroupServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
        this.members = new ConcurrentHashMap<>();
        this.clientStreams = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.running = true;
    }

    public void start() {
        logger.info("Server starting on port {}", port);
        
        // Start heartbeat checker
        scheduler.scheduleAtFixedRate(this::checkHeartbeats, 
            HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);

        // Accept client connections
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting client connection", e);
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        ObjectOutputStream out = null;
        String memberId = null;
        
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            // Read initial join message
            Message joinMessage = (Message) in.readObject();
            if (joinMessage.getType() != Message.Type.JOIN) {
                throw new IllegalStateException("First message must be JOIN");
            }

            memberId = joinMessage.getSenderId();
            
            // Check for duplicate user ID
            if (members.containsKey(memberId)) {
                Message errorMessage = new Message(
                    "SERVER",
                    memberId,
                    Message.Type.ERROR,
                    "User ID already exists. Please choose a different ID."
                );
                out.writeObject(errorMessage);
                out.flush();
                clientSocket.close();
                return;
            }

            Member newMember = new Member(memberId, clientSocket.getInetAddress(), clientSocket.getPort());
            
            // Set as host if first member
            if (members.isEmpty()) {
                newMember.setHost(true);
                logger.info("Member {} is now host", memberId);
            }

            // Add member
            members.put(memberId, newMember);
            clientStreams.put(memberId, out);

            // Notify all members about the new member
            broadcastMemberList();

            // Handle messages from client
            while (running) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }

        } catch (EOFException e) {
            // Client disconnected without proper quit
            logger.info("Client {} disconnected unexpectedly", memberId);
        } catch (IOException | ClassNotFoundException e) {
            logger.info("Client {} connection error: {}", memberId, e.getMessage());
        } finally {
            // Clean up resources
            if (memberId != null) {
                try {
                    handleMemberLeave(memberId);
                    logger.info("Cleaned up resources for member {}", memberId);
                } catch (IOException e) {
                    logger.error("Error cleaning up member {}: {}", memberId, e.getMessage());
                }
            }
            
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Error closing client socket: {}", e.getMessage());
            }
        }
    }

    private void handleMessage(Message message) {
        try {
            switch (message.getType()) {
                case LEAVE:
                    handleMemberLeave(message.getSenderId());
                    break;
                case HEARTBEAT:
                    handleHeartbeat(message.getSenderId());
                    break;
                case PRIVATE:
                    forwardPrivateMessage(message);
                    break;
                case BROADCAST:
                    forwardBroadcastMessage(message);
                    break;
                default:
                    logger.warn("Unhandled message type: {}", message.getType());
            }
        } catch (IOException e) {
            logger.error("Error handling message", e);
        }
    }

    private void handleMemberLeave(String memberId) throws IOException {
        Member leavingMember = members.remove(memberId);
        clientStreams.remove(memberId);

        if (leavingMember != null && leavingMember.host()) {
            // Select new host
            if (!members.isEmpty()) {
                String newHostId = members.keySet().iterator().next();
                Member newHost = members.get(newHostId);
                newHost.setHost(true);
                
                // Notify all members about new host
                broadcastMemberList();
            }
        }
    }

    private void handleHeartbeat(String memberId) {
        Member member = members.get(memberId);
        if (member != null) {
            member.updateHeartbeat();
        }
    }

    private void checkHeartbeats() {
        LocalDateTime now = LocalDateTime.now();
        Set<String> deadMembers = members.entrySet().stream()
            .filter(e -> now.minusSeconds(HEARTBEAT_TIMEOUT).isAfter(e.getValue().getLastHeartbeat()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        deadMembers.forEach(memberId -> {
            try {
                handleMemberLeave(memberId);
            } catch (IOException e) {
                logger.error("Error removing dead member {}", memberId, e);
            }
        });
    }

    private void forwardPrivateMessage(Message message) throws IOException {
        ObjectOutputStream recipientStream = clientStreams.get(message.getRecipientId());
        if (recipientStream != null) {
            recipientStream.writeObject(message);
            recipientStream.flush();
        }
    }

    private void forwardBroadcastMessage(Message message) throws IOException {
        for (Map.Entry<String, ObjectOutputStream> entry : clientStreams.entrySet()) {
            if (!entry.getKey().equals(message.getSenderId())) {
                entry.getValue().writeObject(message);
                entry.getValue().flush();
            }
        }
    }

    private void broadcastMemberList() throws IOException {
        Message memberListMessage = new Message(
            "SERVER",
            null,
            Message.Type.MEMBER_LIST,
            new ArrayList<>(members.values()).toString()
        );

        for (ObjectOutputStream stream : clientStreams.values()) {
            stream.writeObject(memberListMessage);
            stream.flush();
        }
    }

    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }
    }

    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
            GroupServer server = new GroupServer(port);
            server.start();
        } catch (IOException e) {
            logger.error("Error starting server", e);
        }
    }
} 