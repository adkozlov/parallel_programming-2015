package ru.spbau.chat.commons;

import org.jetbrains.annotations.NotNull;
import ru.spbau.chat.commons.protocol.ChatProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.Consumer;

import static ru.spbau.chat.commons.IOUtils.allocateWithDefaultSize;
import static ru.spbau.chat.commons.IOUtils.fromByteBuffer;

/**
 * @author adkozlov
 */
public class ReadCompletionHandler extends AbstractCompletionHandler {

    private final @NotNull ByteBuffer buffer;
    private final @NotNull Consumer<ChatProtocol.Message> onMessage;
    private final @NotNull Consumer<Integer> onError;

    private ReadCompletionHandler(@NotNull ByteBuffer buffer,
                                  @NotNull Consumer<ChatProtocol.Message> onMessage,
                                  @NotNull Consumer<Integer> onError) {
        this.buffer = buffer;
        this.onMessage = onMessage;
        this.onError = onError;
    }

    @Override
    protected void onMessage(@NotNull AsynchronousSocketChannel socketChannel) throws IOException {
        try {
            onMessage.accept(fromByteBuffer(buffer));
        } finally {
            submitReadTask(socketChannel, onMessage, onError);
        }
    }

    @Override
    protected void onError(@NotNull Integer result) {
        onError.accept(result);
    }

    public static void submitReadTask(@NotNull AsynchronousSocketChannel socketChannel,
                                      @NotNull Consumer<ChatProtocol.Message> onMessage,
                                      @NotNull Consumer<Integer> onError) {
        ByteBuffer buffer = allocateWithDefaultSize();
        socketChannel.read(buffer,
                socketChannel,
                new ReadCompletionHandler(buffer, onMessage, onError));
    }
}
