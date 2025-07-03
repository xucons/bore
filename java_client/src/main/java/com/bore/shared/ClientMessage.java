package com.bore.shared;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.UUID;

/**
 * 客户端发送到服务器的消息
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = ClientMessage.ClientMessageSerializer.class)
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
    
    /**
     * 自定义序列化器，用于生成 Rust 格式的消息
     */
    public static class ClientMessageSerializer extends StdSerializer<ClientMessage> {
        
        public ClientMessageSerializer() {
            this(null);
        }
        
        public ClientMessageSerializer(Class<ClientMessage> t) {
            super(t);
        }
        
        @Override
        public void serialize(ClientMessage value, JsonGenerator gen, SerializerProvider provider) 
                throws IOException, JsonProcessingException {
            gen.writeStartObject();
            
            switch (value.type) {
                case AUTHENTICATE:
                    gen.writeStringField("Authenticate", value.authenticateTag);
                    break;
                case HELLO:
                    gen.writeNumberField("Hello", value.helloPort);
                    break;
                case ACCEPT:
                    gen.writeStringField("Accept", value.acceptId.toString());
                    break;
            }
            
            gen.writeEndObject();
        }
    }
}