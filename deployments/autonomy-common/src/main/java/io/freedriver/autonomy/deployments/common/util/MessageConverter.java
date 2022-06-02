package io.freedriver.autonomy.deployments.common.util;

import java.io.IOException;

public interface MessageConverter {
    byte[] toMessage(Object o) throws IOException;
    <T> T fromMessage(byte[] data) throws IOException;
}
