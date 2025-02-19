package com.comp1549.groupchat.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    public enum Type {
        JOIN,           // New member joining
        LEAVE,          // Member leaving
        HOST,           // Host related messages
        HEARTBEAT,      // Heartbeat messages
        PRIVATE,        // Private message between members
        BROADCAST,      // Broadcast message to all members
        MEMBER_LIST,    // List of current members
        ERROR           // Error messages
    }

    private final String senderId;
    private final String recipientId;
    private final Type type;
    private final String content;
    private final LocalDateTime timestamp;

    public Message(String senderId, String recipientId, Type type, String content) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.type = type;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public static Message createBroadcast(String senderId, String content) {
        return new Message(senderId, null, Type.BROADCAST, content);
    }

    public static Message createPrivate(String senderId, String recipientId, String content) {
        return new Message(senderId, recipientId, Type.PRIVATE, content);
    }

    public static Message createHeartbeat(String senderId) {
        return new Message(senderId, null, Type.HEARTBEAT, "");
    }

    public String getSenderId() {
        return senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isBroadcast() {
        return type == Type.BROADCAST;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s): %s",
            timestamp, senderId, recipientId != null ? recipientId : "ALL", type, content);
    }
} 