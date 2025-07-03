package com.bore;

import com.bore.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "bore-client", mixinStandardHelpOptions = true, 
         description = "Java client for bore tunnel service")
public class Main implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Parameters(index = "0", description = "The local port to expose")
    private int localPort;

    @Option(names = {"-l", "--local-host"}, description = "The local host to expose", defaultValue = "localhost")
    private String localHost;

    @Option(names = {"-t", "--to"}, description = "Address of the remote server", required = true)
    private String to;

    @Option(names = {"-p", "--port"}, description = "Optional port on the remote server to select", defaultValue = "0")
    private int port;

    @Option(names = {"-s", "--secret"}, description = "Optional secret for authentication")
    private String secret;

    public static void main(String[] args) {
//        int exitCode = new CommandLine(new Main()).execute(args);
        int exitCode = 0;
        try {
            Client client = Client.create("localhost", 8080, "localhost", 9527, "aabb@123");

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down client...");
                client.close();
            }));

            client.listen();
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            Client client = Client.create(localHost, localPort, to, port, secret);
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down client...");
                client.close();
            }));
            
            client.listen();
            return 0;
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            return 1;
        }
    }
}