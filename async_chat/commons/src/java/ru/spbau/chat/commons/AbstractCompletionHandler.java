package ru.spbau.chat.commons;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.function.Consumer;

/**
 * @author adkozlov
 */
public abstract class AbstractCompletionHandler implements CompletionHandler<Integer, AsynchronousSocketChannel> {

    protected final @NotNull ByteBuffer buffer;
    protected final @NotNull Consumer<Throwable> onError;

    protected AbstractCompletionHandler(@NotNull ByteBuffer buffer,
                                        @NotNull Consumer<Throwable> onError) {
        this.buffer = buffer;
        this.onError = onError;
    }

    protected abstract void onMessage(@NotNull AsynchronousSocketChannel channel);

    @Override
    public void completed(@NotNull Integer bytes, @NotNull AsynchronousSocketChannel channel) {
        if (bytes != -1) {
            onMessage(channel);
        } else {
            failed(new ClosedChannelException(), channel);
        }
    }

    @Override
    public void failed(@NotNull Throwable throwable, @NotNull AsynchronousSocketChannel channel) {
        onError.accept(throwable);
    }
}
