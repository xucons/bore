package com.bore.shared;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * 使用空字符分隔的JSON帧传输流
 */
public class Delimited implements Closeable {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final ObjectMapper objectMapper;
    private final ByteArrayOutputStream readBuffer;
    private final ExecutorService executor;

    public Delimited(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.objectMapper = new ObjectMapper();
        this.readBuffer = new ByteArrayOutputStream();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 从流中读取下一个空字符分隔的JSON指令
     */
    public <T> T recv(Class<T> type) throws IOException {
        readBuffer.reset();
        int b;
        while ((b = input.read()) != 0) {
            if (b == -1) {
                return null; // EOF
            }
            readBuffer.write(b);
            if (readBuffer.size() > Constants.MAX_FRAME_LENGTH) {
                throw new IOException("Frame too large");
            }
        }

        byte[] data = readBuffer.toByteArray();
        if (data.length == 0) {
            return null;
        }

        return objectMapper.readValue(data, type);
    }

    /**
     * 从流中读取下一个空字符分隔的JSON指令，带有默认超时
     */
    public <T> T recvTimeout(Class<T> type) throws IOException, TimeoutException {
        Future<T> future = executor.submit(() -> recv(type));
        try {
            return future.get(Constants.NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for message", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Error receiving message", e);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Timed out waiting for initial message");
        }
    }

    /**
     * 在流上发送空字符终止的JSON指令
     */
    public void send(Object msg) throws IOException {
        byte[] data = objectMapper.writeValueAsBytes(msg);
        output.write(data);
        output.write(0); // 空字符分隔符
        output.flush();
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        input.close();
        output.close();
        socket.close();
    }

    /**
     * 获取底层Socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * 获取输入流中的可用数据
     */
    public byte[] getAvailableData() throws IOException {
        int available = input.available();
        if (available <= 0) {
            return new byte[0];
        }

        byte[] data = new byte[available];
        input.readFully(data);
        return data;
    }
}