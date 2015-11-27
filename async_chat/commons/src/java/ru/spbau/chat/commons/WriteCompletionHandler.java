package ru.spbau.chat.commons;

import org.jetbrains.annotations.NotNull;
import ru.spbau.chat.commons.protocol.ChatProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import static ru.spbau.chat.commons.IOUtils.toByteBuffer;

/**
 * @author adkozlov
 */
public class WriteCompletionHandler extends AbstractCompletionHandler {

    private WriteCompletionHandler() {
    }

    @Override
    protected void onError(@NotNull Integer result) throws IOException {
        throw new IOException("result code on write: " + result);
    }

    public static void submitWriteTask(@NotNull AsynchronousSocketChannel socketChannel,
                                       @NotNull ByteBuffer buffer) {
        socketChannel.write(buffer, socketChannel, new WriteCompletionHandler());
    }

    public static void submitWriteTask(@NotNull AsynchronousSocketChannel socketChannel,
                                       @NotNull ChatProtocol.Message message) {
        submitWriteTask(socketChannel, toByteBuffer(message));
    }
}
