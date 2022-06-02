package io.freedriver.autonomy.deployments.common.util;

public class JacksonMessageConverterTest extends MessageConverterTest {

    private static final JacksonMessaging converter = new JacksonMessaging();

    @Override
    protected MessageConverter getConverter() {
        return converter;
    }
}
