package com.comp1549.groupchat.model;

import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MemberTest {

    @Test
    void testMemberCreation() throws Exception {
        String id = "test1";
        InetAddress ip = InetAddress.getLocalHost();
        int port = 8080;

        Member member = new Member(id, ip, port);

        assertAll(
            () -> assertEquals(id, member.getId()),
            () -> assertEquals(ip, member.getip()),
            () -> assertEquals(port, member.getPort()),
            () -> assertFalse(member.host())
        );
    }

    @Test
    void testHostStatus() throws Exception {
        Member member = new Member("test1", InetAddress.getLocalHost(), 8080);
        assertFalse(member.host());

        member.setHost(true);
        assertTrue(member.host());

        member.setHost(false);
        assertFalse(member.host());
    }

    @Test
    void testHeartbeatUpdate() throws Exception {
        Member member = new Member("test1", InetAddress.getLocalHost(), 8080);
        LocalDateTime initialHeartbeat = member.getLastHeartbeat();
        
        // Wait a small amount of time
        Thread.sleep(100);
        
        member.updateHeartbeat();
        LocalDateTime updatedHeartbeat = member.getLastHeartbeat();
        
        assertTrue(updatedHeartbeat.isAfter(initialHeartbeat));
    }

    @Test
    void testEquality() throws Exception {
        InetAddress ip = InetAddress.getLocalHost();
        Member member1 = new Member("test1", ip, 8080);
        Member member2 = new Member("test1", ip, 8080);
        Member member3 = new Member("test2", ip, 8080);
        Member member4 = new Member("test1", ip, 8081);

        assertAll(
            () -> assertEquals(member1, member2),
            () -> assertNotEquals(member1, member3),
            () -> assertNotEquals(member1, member4),
            () -> assertEquals(member1.hashCode(), member2.hashCode())
        );
    }

    @Test
    void testToString() throws Exception {
        Member member = new Member("test1", InetAddress.getLocalHost(), 8080);
        String toString = member.toString();

        assertAll(
            () -> assertTrue(toString.contains("test1")),
            () -> assertTrue(toString.contains("8080")),
            () -> assertTrue(toString.contains(InetAddress.getLocalHost().getHostAddress()))
        );
    }
} 