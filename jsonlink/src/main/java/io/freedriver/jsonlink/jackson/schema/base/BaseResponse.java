package io.freedriver.jsonlink.jackson.schema.base;

/**
 * The base-level response object, including the version of the schema.
 */
public class BaseResponse {
    private Version version;

    public BaseResponse() {
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
