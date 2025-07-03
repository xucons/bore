package com.bore.auth;

import com.bore.shared.ClientMessage;
import com.bore.shared.Delimited;
import com.bore.shared.ServerMessage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * 用于认证具有密钥的客户端的MAC包装器
 */
public class Authenticator {
    private final Mac hmac;

    /**
     * 从密钥生成认证器
     */
    public Authenticator(String secret) {
        try {
            // 对密钥进行SHA-256哈希处理
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedSecret = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            
            // 创建HMAC-SHA256实例
            SecretKeySpec keySpec = new SecretKeySpec(hashedSecret, "HmacSHA256");
            hmac = Mac.getInstance("HmacSHA256");
            hmac.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to initialize authenticator", e);
        }
    }

    /**
     * 为挑战生成回复消息
     */
    public String answer(UUID challenge) {
        byte[] challengeBytes = uuidToBytes(challenge);
        byte[] hmacResult;
        synchronized (hmac) {
            hmacResult = hmac.doFinal(challengeBytes);
        }
        return bytesToHex(hmacResult);
    }

    /**
     * 验证对挑战的回复
     */
    public boolean validate(UUID challenge, String tag) {
        try {
            byte[] tagBytes = hexToBytes(tag);
            byte[] challengeBytes = uuidToBytes(challenge);
            byte[] expectedTag;
            synchronized (hmac) {
                expectedTag = hmac.doFinal(challengeBytes);
            }
            
            // 常量时间比较以防止计时攻击
            return constantTimeEquals(tagBytes, expectedTag);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 作为客户端，回答挑战以尝试向服务器进行身份验证
     */
    public void clientHandshake(Delimited stream) throws IOException, TimeoutException {
        ServerMessage message = stream.recvTimeout(ServerMessage.class);
        if (message == null || message.getType() != ServerMessage.MessageType.CHALLENGE) {
            throw new IOException("Expected authentication challenge, but no secret was required");
        }
        
        UUID challenge = message.getChallengeId();
        String tag = answer(challenge);
        stream.send(ClientMessage.authenticate(tag));
    }

    // 辅助方法
    private static byte[] uuidToBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];
        
        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> ((7 - i) * 8));
        }
        for (int i = 0; i < 8; i++) {
            buffer[i + 8] = (byte) (lsb >>> ((7 - i) * 8));
        }
        
        return buffer;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            result[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return result;
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}