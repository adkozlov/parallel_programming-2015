package ru.spbau.chat.server;

import com.google.protobuf.ProtocolStringList;
import org.jetbrains.annotations.NotNull;
import ru.spbau.chat.commons.protocol.ChatProtocol;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.spbau.chat.commons.ReadCompletionHandler.submitReadTask;
import static ru.spbau.chat.commons.WriteCompletionHandler.submitWriteTask;
import static ru.spbau.chat.commons.protocol.ChatProtocol.Message.newBuilder;

/**
 * @author adkozlov
 */
public class Server implements Closeable {

    private static final @NotNull Logger LOGGER = Logger.getLogger(Server.class.getName());

    private final @NotNull AsynchronousChannelGroup channelGroup;
    private final @NotNull AsynchronousServerSocketChannel serverSocketChannel;
    private final @NotNull ConcurrentHashMap<AsynchronousSocketChannel, Long> socketChannels = new ConcurrentHashMap<>();
    private final @NotNull ExecutorService executorService = Executors.newSingleThreadExecutor();

    public Server(int port, int poolSize) throws IOException {
        channelGroup = AsynchronousChannelGroup.withFixedThreadPool(poolSize - 1, Executors.defaultThreadFactory());

        serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverSocketChannel.bind(new InetSocketAddress("localhost", port));
    }

    @Override
    public void close() throws IOException {
        serverSocketChannel.close();
        channelGroup.shutdownNow();
    }

    public void accept() {
        try {
            AsynchronousSocketChannel channel = serverSocketChannel.accept().get();
            socketChannels.put(channel, System.currentTimeMillis());

            submitReadTask(channel,
                    throwable -> socketChannels.remove(channel),
                    message -> {
                        switch (message.getType()) {
                            case MESSAGE:
                                broadcastMessage(message, channel);
                                break;
                            case COMMAND:
                                handleCommand(message.getTextList(), channel);
                                break;
                        }
                    }
            );
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "channel is not open", e);
        }
    }

    private void broadcastMessage(@NotNull ChatProtocol.Message message,
                                  @NotNull AsynchronousSocketChannel filter) {
        socketChannels.forEachKey(1,
                channel -> !channel.equals(filter) ? channel : null,
                channel -> sendMessage(message, channel));
    }

    private void handleCommand(@NotNull ProtocolStringList command, @NotNull AsynchronousSocketChannel channel) {
        StringBuilder stringBuilder = new StringBuilder();
        command.forEach(line -> stringBuilder.append(line).append(System.lineSeparator()));

        executorService.submit(() -> sendMessage(newBuilder()
                        .setType(ChatProtocol.Message.Type.COMMAND)
                        .addAllText(execute(stringBuilder.toString()))
                        .build(),
                channel));
    }

    public static @NotNull List<String> execute(@NotNull String command) {
        List<String> result = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                bufferedReader.lines().forEach(result::add);
            }
        } catch (IOException | InterruptedException e) {
            result.add(e.getMessage());
        }
        return result;
    }

    private static void sendMessage(@NotNull ChatProtocol.Message message, @NotNull AsynchronousSocketChannel channel) {
        submitWriteTask(channel, message, throwable -> LOGGER.log(Level.SEVERE, "send message error", throwable));
    }

    public static void main(@NotNull String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        try (Server server = new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]))) {
            while (!Thread.interrupted()) {
                server.accept();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "server is closed", e);
        } catch (NumberFormatException e) {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("usage: Server <port> <pool_size>");
    }
}
