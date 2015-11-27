package ru.spbau.chat.commons;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import static ru.spbau.chat.commons.IOUtils.handleError;

/**
 * @author adkozlov
 */
public abstract class AbstractCompletionHandler implements CompletionHandler<Integer, AsynchronousSocketChannel> {

    protected void onMessage(@NotNull AsynchronousSocketChannel socketChannel) throws IOException {
    }

    protected void onError(@NotNull Integer result) throws IOException {
    }

    @Override
    public void completed(@NotNull Integer result, @NotNull AsynchronousSocketChannel socketChannel) {
        try {
            if (result > 0) {
                onMessage(socketChannel);
            } else {
                onError(result);
            }
        } catch (IOException e) {
            failed(e, socketChannel);
        }
    }

    @Override
    public void failed(@NotNull Throwable throwable, @NotNull AsynchronousSocketChannel socketChannel) {
        handleError(throwable);
    }
}
