package ru.spbau.chat.commons;

import org.jetbrains.annotations.NotNull;
import ru.spbau.chat.commons.protocol.ChatProtocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.Consumer;

/**
 * @author adkozlov
 */
public class WriteCompletionHandler extends AbstractCompletionHandler {

    private WriteCompletionHandler(@NotNull ByteBuffer buffer,
                                   @NotNull Consumer<Throwable> onError,
                                   @NotNull AsynchronousSocketChannel channel) {
        super(buffer, onError);
        channel.write(buffer, channel, this);
    }

    @Override
    protected void onMessage(@NotNull AsynchronousSocketChannel channel) {
        if (buffer.hasRemaining()) {
            new WriteCompletionHandler(buffer, onError, channel);
        }
    }

    public static void submitWriteTask(@NotNull AsynchronousSocketChannel channel,
                                       @NotNull ChatProtocol.Message message,
                                       @NotNull Consumer<Throwable> onError) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeDelimitedTo(outputStream);
            new WriteCompletionHandler(ByteBuffer.wrap(outputStream.toByteArray()), onError, channel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
