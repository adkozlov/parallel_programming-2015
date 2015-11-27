package ru.spbau.chat.server;

import com.google.protobuf.ProtocolStringList;
import org.jetbrains.annotations.NotNull;
import ru.spbau.chat.commons.protocol.ChatProtocol;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ru.spbau.chat.commons.IOUtils.handleError;
import static ru.spbau.chat.commons.IOUtils.toByteBuffer;
import static ru.spbau.chat.commons.ReadCompletionHandler.submitReadTask;
import static ru.spbau.chat.commons.WriteCompletionHandler.submitWriteTask;

/**
 * @author adkozlov
 */
public class Server implements Closeable {

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
            AsynchronousSocketChannel socketChannel = serverSocketChannel.accept().get();
            socketChannels.put(socketChannel, System.currentTimeMillis());

            submitReadTask(socketChannel,
                    message -> {
                        switch (message.getType()) {
                            case MESSAGE:
                                broadcastMessage(message, socketChannel);
                                break;
                            case COMMAND:
                                handleCommand(message.getTextList(), socketChannel);
                                break;
                        }
                    },
                    result -> socketChannels.remove(socketChannel));
        } catch (InterruptedException | ExecutionException e) {
            handleError(e);
        }
    }

    private void broadcastMessage(@NotNull ChatProtocol.Message message,
                                  @NotNull AsynchronousSocketChannel filter) {
        ByteBuffer buffer = toByteBuffer(message);
        socketChannels.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(channel -> !channel.equals(filter))
                .forEach(channel -> submitWriteTask(channel, buffer));
    }

    private void handleCommand(@NotNull ProtocolStringList command, @NotNull AsynchronousSocketChannel socketChannel) {
        StringBuilder stringBuilder = new StringBuilder();
        command.forEach(line -> stringBuilder.append(line).append(System.lineSeparator()));

        executorService.submit(() -> {
            List<String> text = new ArrayList<>();
            try {
                Process process = Runtime.getRuntime().exec(stringBuilder.toString());
                process.waitFor();
                text.addAll(lines(process.getInputStream()));
            } catch (IOException | InterruptedException e) {
                text.add(e.getMessage());
            } finally {
                submitWriteTask(socketChannel, buildMessage(text));
            }
        });
    }

    private static @NotNull ChatProtocol.Message buildMessage(@NotNull List<String> text) {
        return ChatProtocol.Message.newBuilder()
                .setType(ChatProtocol.Message.Type.COMMAND)
                .addAllText(text)
                .build();
    }

    private static @NotNull List<String> lines(@NotNull InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            return bufferedReader.lines().collect(Collectors.toList());
        }
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
            handleError(e);
        } catch (NumberFormatException e) {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("usage: Server <port> <pool_size>");
    }
}
