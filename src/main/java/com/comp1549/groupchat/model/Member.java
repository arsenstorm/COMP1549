package com.comp1549.groupchat.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;

public class Member implements Serializable {
    private final String id;
    private final InetAddress ip;
    private final int port;
    private boolean host;
    private LocalDateTime lastHeartbeat;

    public Member(String id, InetAddress ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.host = false;
        this.lastHeartbeat = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public InetAddress getip() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean host() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return port == member.port && Objects.equals(id, member.id) && Objects.equals(ip, member.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ip, port);
    }

    @Override
    public String toString() {
        return String.format("Member{id='%s', ip=%s, port=%d, host=%s}", 
            id, ip.getHostAddress(), port, host);
    }
} 