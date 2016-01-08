package ru.spbau.chat.commons;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.spbau.chat.commons.protocol.ChatProtocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.Consumer;

import static java.nio.ByteBuffer.allocate;
import static ru.spbau.chat.commons.protocol.ChatProtocol.Message.parseDelimitedFrom;

/**
 * @author adkozlov
 */
public class ReadCompletionHandler extends AbstractCompletionHandler {

    private static final int BUFFER_SIZE = 1024;

    private final @NotNull ByteBuffer suffix;
    private final @NotNull Consumer<ChatProtocol.Message> onMessage;

    private @Nullable ByteBuffer buffer;

    private ReadCompletionHandler(@NotNull ByteBuffer prefix,
                                  @NotNull Consumer<Throwable> onError,
                                  @NotNull Consumer<ChatProtocol.Message> onMessage,
                                  @NotNull AsynchronousSocketChannel channel) {
        super(prefix, onError);
        this.suffix = allocate(BUFFER_SIZE);
        this.onMessage = onMessage;

        channel.read(suffix, channel, this);
    }

    @Override
    protected void onMessage(@NotNull AsynchronousSocketChannel channel) {
        ByteBuffer buffer = getBuffer();
        try {
            onMessage.accept(parseDelimitedFrom(new ByteArrayInputStream(buffer.array())));
        } catch (IOException e) {
            buffer.position(0);
        } finally {
            new ReadCompletionHandler(buffer, onError, onMessage, channel);
        }
    }

    private @NotNull ByteBuffer getBuffer() {
        if (buffer == null) {
            ByteBuffer prefix = super.buffer;
            prefix.flip();
            suffix.flip();

            buffer = allocate(prefix.remaining() + suffix.remaining());
            buffer.put(prefix).put(suffix);
            buffer.clear();
        }
        return buffer;
    }

    public static void submitReadTask(@NotNull AsynchronousSocketChannel channel,
                                      @NotNull Consumer<Throwable> onError,
                                      @NotNull Consumer<ChatProtocol.Message> onMessage) {
        new ReadCompletionHandler(allocate(0), onError, onMessage, channel);
    }
}
