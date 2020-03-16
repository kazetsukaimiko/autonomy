package io.freedriver.jsonlink.config;

public interface Migration<VN extends ConfigFile> {
    VN migrate();
}
