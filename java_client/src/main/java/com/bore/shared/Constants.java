package com.bore.shared;

/**
 * 共享常量
 */
public class Constants {
    // TCP端口用于与服务器的控制连接
    public static final int CONTROL_PORT = 7835;
    
    // JSON帧在流中的最大字节长度
    public static final int MAX_FRAME_LENGTH = 256;
    
    // 网络连接和初始协议消息的超时时间（毫秒）
    public static final int NETWORK_TIMEOUT_MS = 3000;
}