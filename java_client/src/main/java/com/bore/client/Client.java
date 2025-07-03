package com.bore.client;

import com.bore.auth.Authenticator;
import com.bore.shared.ClientMessage;
import com.bore.shared.Constants;
import com.bore.shared.Delimited;
import com.bore.shared.ServerMessage;
import com.bore.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 客户端的状态结构
 */
public class Client implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private Delimited conn;
    private final String to;
    private final String localHost;
    private final int localPort;
    private final int remotePort;
    private final Authenticator auth;
    private final ExecutorService executor;
    private volatile boolean running = true;

    /**
     * 创建一个新的客户端
     */
    public static Client create(String localHost, int localPort, String to, int port, String secret) 
            throws IOException, TimeoutException {
        Socket socket = StreamUtils.connectWithTimeout(to, Constants.CONTROL_PORT, Constants.NETWORK_TIMEOUT_MS);
        Delimited stream = new Delimited(socket);
        
        Authenticator auth = null;
        if (secret != null && !secret.isEmpty()) {
            auth = new Authenticator(secret);
            auth.clientHandshake(stream);
        }

        stream.send(ClientMessage.hello(port));
        ServerMessage response = stream.recvTimeout(ServerMessage.class);
        
        if (response == null) {
            throw new IOException("Unexpected EOF");
        }
        
        switch (response.getType()) {
            case HELLO:
                int remotePort = response.getHelloPort();
                logger.info("Connected to server, remote port: {}", remotePort);
                logger.info("Listening at {}:{}", to, remotePort);
                return new Client(stream, to, localHost, localPort, remotePort, auth);
                
            case ERROR:
                throw new IOException("Server error: " + response.getErrorMessage());
                
            case CHALLENGE:
                throw new IOException("Server requires authentication, but no client secret was provided");
                
            default:
                throw new IOException("Unexpected initial non-hello message");
        }
    }

    private Client(Delimited conn, String to, String localHost, int localPort, int remotePort, Authenticator auth) {
        this.conn = conn;
        this.to = to;
        this.localHost = localHost;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.auth = auth;
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * 返回远程上公开可用的端口
     */
    public int getRemotePort() {
        return remotePort;
    }

    /**
     * 启动客户端，监听新连接
     */
    public void listen() throws IOException {
        try {
            while (running) {
                ServerMessage message = conn.recv(ServerMessage.class);
                if (message == null) {
                    break; // EOF
                }
                
                switch (message.getType()) {
                    case HELLO:
                        logger.warn("Unexpected hello");
                        break;
                        
                    case CHALLENGE:
                        logger.warn("Unexpected challenge");
                        break;
                        
                    case HEARTBEAT:
                        // 心跳包，不需要处理
                        break;
                        
                    case CONNECTION:
                        UUID id = message.getConnectionId();
                        executor.submit(() -> handleConnection(id));
                        break;
                        
                    case ERROR:
                        logger.error("Server error: {}", message.getErrorMessage());
                        break;
                }
            }
        } finally {
            close();
        }
    }

    private void handleConnection(UUID id) {
        logger.info("New connection: {}", id);
        
        try {
            // 连接到服务器的控制端口
            Socket remoteConn = StreamUtils.connectWithTimeout(to, Constants.CONTROL_PORT, Constants.NETWORK_TIMEOUT_MS);
            Delimited remoteStream = new Delimited(remoteConn);
            
            // 如果需要，进行认证
            if (auth != null) {
                auth.clientHandshake(remoteStream);
            }
            
            // 发送接受连接的消息
            remoteStream.send(ClientMessage.accept(id));
            
            // 连接到本地服务
            Socket localConn = StreamUtils.connectWithTimeout(localHost, localPort, Constants.NETWORK_TIMEOUT_MS);
            
            // 将任何缓冲数据写入本地连接
            byte[] bufferedData = remoteStream.getAvailableData();
            if (bufferedData.length > 0) {
                localConn.getOutputStream().write(bufferedData);
                localConn.getOutputStream().flush();
            }
            
            // 在两个连接之间双向复制数据
            StreamUtils.copyBidirectional(localConn, remoteConn);
            
            logger.info("Connection exited: {}", id);
        } catch (Exception e) {
            logger.warn("Connection exited with error: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        running = false;
        if (conn != null) {
            try {
                conn.close();
            } catch (IOException e) {
                // 忽略关闭错误
            }
            conn = null;
        }
        
        executor.shutdownNow();
    }
}