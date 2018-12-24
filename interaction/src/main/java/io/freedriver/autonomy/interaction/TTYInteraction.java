package io.freedriver.autonomy.interaction;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;

public class TTYInteraction {
    private final Path devicePath;

    public TTYInteraction(Path devicePath) {
        this.devicePath = devicePath;
    }

    public int getServoValue(int channel) throws IOException {
        SeekableByteChannel seekableByteChannel = Files.newByteChannel(devicePath, new HashSet<>(Arrays.asList(
                StandardOpenOption.WRITE, StandardOpenOption.READ
        )));

        ByteBuffer bb = ByteBuffer.wrap(MaestroInteraction.getPositionCommandBytes(channel));

        int ret = seekableByteChannel.write(bb);

        ByteBuffer rb = ByteBuffer.allocate(2);
        seekableByteChannel.read(rb);

        rb.rewind();
        return rb.getInt();


    }
}
