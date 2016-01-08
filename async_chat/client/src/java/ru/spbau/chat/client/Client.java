package ru.spbau.chat.client;

import org.jetbrains.annotations.NotNull;
import ru.spbau.chat.commons.protocol.ChatProtocol;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.spbau.chat.commons.ReadCompletionHandler.submitReadTask;
import static ru.spbau.chat.commons.WriteCompletionHandler.submitWriteTask;
import static ru.spbau.chat.commons.protocol.ChatProtocol.Message.newBuilder;

/**
 * @author adkozlov
 */
public class Client implements Closeable {

    private static final @NotNull Logger LOGGER = Logger.getLogger(Client.class.getName());
    private static final @NotNull Pattern COMMAND_REGEX = Pattern.compile("^command\\s");

    private final @NotNull AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open();
    private final @NotNull String author;

    public Client(@NotNull String host, int port, @NotNull String author)
            throws IOException, ExecutionException, InterruptedException {
        this.author = author;

        socketChannel.connect(new InetSocketAddress(host, port)).get();

        submitReadTask(socketChannel,
                throwable -> LOGGER.log(Level.SEVERE, "read message error", throwable),
                message -> {
                    switch (message.getType()) {
                        case MESSAGE:
                            System.out.printf("%s: ", message.getAuthor());
                        default:
                            message.getTextList().forEach(System.out::println);
                    }
                }
        );
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }

    public void writeMessage(@NotNull String text) {
        submitWriteTask(socketChannel, buildMessage(text),
                throwable -> LOGGER.log(Level.SEVERE, "send message error", throwable));
    }

    private @NotNull ChatProtocol.Message buildMessage(@NotNull String text) {
        Matcher matcher = COMMAND_REGEX.matcher(text);
        boolean isCommand = matcher.find();
        return newBuilder()
                .setType(isCommand ? ChatProtocol.Message.Type.COMMAND : ChatProtocol.Message.Type.MESSAGE)
                .addText(isCommand ? matcher.replaceFirst("") : text)
                .setAuthor(author)
                .build();
    }

    public static void main(@NotNull String[] args) {
        if (args.length < 3) {
            printUsage();
            return;
        }

        try (Client client = new Client(args[0], Integer.parseInt(args[1]), args[2]);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
            bufferedReader.lines().forEach(client::writeMessage);
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "client is closed", e);
        } catch (NumberFormatException e) {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("usage: Client <host> <port> <user_name>");
    }
}
