package com.bore.shared;

import java.util.UUID;

/**
 * 服务器发送到客户端的消息
 */
public class ServerMessage {
    private MessageType type;
    private UUID challengeId;
    private int helloPort;
    private UUID connectionId;
    private String errorMessage;

    public enum MessageType {
        CHALLENGE,
        HELLO,
        HEARTBEAT,
        CONNECTION,
        ERROR
    }

    // 私有构造函数
    private ServerMessage() {}

    // 创建Challenge消息
    public static ServerMessage challenge(UUID id) {
        ServerMessage message = new ServerMessage();
        message.type = MessageType.CHALLENGE;
        message.challengeId = id;
        return message;
    }

    // 创建Hello消息
    public static ServerMessage hello(int port) {
        ServerMessage message = new ServerMessage();
        message.type = MessageType.HELLO;
        message.helloPort = port;
        return message;
    }

    // 创建Heartbeat消息
    public static ServerMessage heartbeat() {
        ServerMessage message = new ServerMessage();
        message.type = MessageType.HEARTBEAT;
        return message;
    }

    // 创建Connection消息
    public static ServerMessage connection(UUID id) {
        ServerMessage message = new ServerMessage();
        message.type = MessageType.CONNECTION;
        message.connectionId = id;
        return message;
    }

    // 创建Error消息
    public static ServerMessage error(String message) {
        ServerMessage serverMessage = new ServerMessage();
        serverMessage.type = MessageType.ERROR;
        serverMessage.errorMessage = message;
        return serverMessage;
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public int getHelloPort() {
        return helloPort;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}