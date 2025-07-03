package com.bore.shared;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.UUID;

/**
 * 服务器发送到客户端的消息
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = ServerMessage.ServerMessageDeserializer.class)
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
    
    /**
     * 自定义反序列化器，用于处理 Rust 格式的消息
     */
    public static class ServerMessageDeserializer extends StdDeserializer<ServerMessage> {
        
        public ServerMessageDeserializer() {
            this(null);
        }
        
        public ServerMessageDeserializer(Class<?> vc) {
            super(vc);
        }
        
        @Override
        public ServerMessage deserialize(JsonParser jp, DeserializationContext ctxt) 
                throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);

            ServerMessage message = new ServerMessage();
            // 检查是否有 Challenge 字段
            if (node.has("Challenge")) {
                message.type = MessageType.CHALLENGE;
                message.challengeId = UUID.fromString(node.get("Challenge").asText());
            }

            // 检查是否有 Hello 字段
            if (node.has("Hello")) {
                message.type = MessageType.HELLO;
                message.helloPort = node.get("Hello").asInt();
            }
            
            // 检查是否有 Heartbeat 字段
            if (node.has("Heartbeat")) {
                message.type = MessageType.HEARTBEAT;
            }
            
            // 检查是否有 Connection 字段
            if (node.has("Connection")) {
                message.type = MessageType.CONNECTION;
                message.connectionId = UUID.fromString(node.get("Connection").asText());
            }
            
            // 检查是否有 Error 字段
            if (node.has("Error")) {
                message.type = MessageType.ERROR;
                message.errorMessage = node.get("Error").asText();
            }
            return message;
        }
    }
}