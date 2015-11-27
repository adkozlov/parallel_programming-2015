package ru.spbau.chat.commons;

import org.jetbrains.annotations.NotNull;
import ru.spbau.chat.commons.protocol.ChatProtocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author adkozlov
 */
public class IOUtils {

    public static final int BUFFER_SIZE = 1024;

    private IOUtils() {
    }

    public static @NotNull ByteBuffer allocateWithDefaultSize() {
        return ByteBuffer.allocate(BUFFER_SIZE);
    }

    public static @NotNull ChatProtocol.Message fromByteBuffer(@NotNull ByteBuffer buffer) throws IOException {
        return ChatProtocol.Message.parseDelimitedFrom(new ByteArrayInputStream(buffer.array()));
    }

    public static @NotNull ByteBuffer toByteBuffer(@NotNull ChatProtocol.Message message) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeDelimitedTo(outputStream);
            return ByteBuffer.wrap(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleError(@NotNull Throwable throwable) {
        System.err.println(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        Arrays.stream(throwable.getSuppressed()).forEach(IOUtils::handleError);
    }
}
