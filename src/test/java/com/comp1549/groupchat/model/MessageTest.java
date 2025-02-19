package com.comp1549.groupchat.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testCreateBroadcast() {
        String senderId = "user1";
        String content = "Hello everyone!";
        
        Message message = Message.createBroadcast(senderId, content);
        
        assertAll(
            () -> assertEquals(senderId, message.getSenderId()),
            () -> assertNull(message.getRecipientId()),
            () -> assertEquals(Message.Type.BROADCAST, message.getType()),
            () -> assertEquals(content, message.getContent()),
            () -> assertTrue(message.isBroadcast())
        );
    }

    @Test
    void testCreatePrivate() {
        String senderId = "user1";
        String recipientId = "user2";
        String content = "Hello user2!";
        
        Message message = Message.createPrivate(senderId, recipientId, content);
        
        assertAll(
            () -> assertEquals(senderId, message.getSenderId()),
            () -> assertEquals(recipientId, message.getRecipientId()),
            () -> assertEquals(Message.Type.PRIVATE, message.getType()),
            () -> assertEquals(content, message.getContent()),
            () -> assertFalse(message.isBroadcast())
        );
    }

    @Test
    void testCreateHeartbeat() {
        String senderId = "user1";
        
        Message message = Message.createHeartbeat(senderId);
        
        assertAll(
            () -> assertEquals(senderId, message.getSenderId()),
            () -> assertNull(message.getRecipientId()),
            () -> assertEquals(Message.Type.HEARTBEAT, message.getType()),
            () -> assertTrue(message.getContent().isEmpty()),
            () -> assertFalse(message.isBroadcast())
        );
    }

    @Test
    void testToString() {
        String senderId = "user1";
        String recipientId = "user2";
        String content = "Test message";
        Message message = new Message(senderId, recipientId, Message.Type.PRIVATE, content);
        
        String toString = message.toString();
        
        assertTrue(toString.contains(senderId));
        assertTrue(toString.contains(recipientId));
        assertTrue(toString.contains(Message.Type.PRIVATE.toString()));
        assertTrue(toString.contains(content));
    }
} 