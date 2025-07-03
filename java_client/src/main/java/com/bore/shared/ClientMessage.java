package com.bore.shared;

import java.util.UUID;

/**
 * 客户端发送到服务器的消息
 */
public class ClientMessage {
    private MessageType type;
    private String authenticateTag;
    private int helloPort;
    private UUID acceptId;

    public enum MessageType {
        AUTHENTICATE,
        HELLO,
        ACCEPT
    }

    // 私有构造函数
    private ClientMessage() {}

    // 创建认证消息
    public static ClientMessage authenticate(String tag) {
        ClientMessage message = new ClientMessage();
        message.type = MessageType.AUTHENTICATE;
        message.authenticateTag = tag;
        return message;
    }

    // 创建Hello消息
    public static ClientMessage hello(int port) {
        ClientMessage message = new ClientMessage();
        message.type = MessageType.HELLO;
        message.helloPort = port;
        return message;
    }

    // 创建Accept消息
    public static ClientMessage accept(UUID id) {
        ClientMessage message = new ClientMessage();
        message.type = MessageType.ACCEPT;
        message.acceptId = id;
        return message;
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public String getAuthenticateTag() {
        return authenticateTag;
    }

    public int getHelloPort() {
        return helloPort;
    }

    public UUID getAcceptId() {
        return acceptId;
    }
}