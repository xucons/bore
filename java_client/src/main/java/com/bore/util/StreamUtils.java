package com.bore.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 流处理工具类
 */
public class StreamUtils {
    /**
     * 在两个流之间双向复制数据
     */
    public static void copyBidirectional(Socket socket1, Socket socket2) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        try {
            // 从socket1到socket2
            executor.submit(() -> {
                try {
                    copyStream(socket1.getInputStream(), socket2.getOutputStream());
                } catch (IOException e) {
                    // 连接可能已关闭，这是预期的
                }
                return null;
            });
            
            // 从socket2到socket1
            executor.submit(() -> {
                try {
                    copyStream(socket2.getInputStream(), socket1.getOutputStream());
                } catch (IOException e) {
                    // 连接可能已关闭，这是预期的
                }
                return null;
            });
            
            // 等待两个任务完成或超时
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 将一个流的内容复制到另一个流
     */
    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
            output.flush();
        }
    }

    /**
     * 带超时的连接
     */
    public static Socket connectWithTimeout(String host, int port, int timeoutMs) throws IOException {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
        return socket;
    }
}