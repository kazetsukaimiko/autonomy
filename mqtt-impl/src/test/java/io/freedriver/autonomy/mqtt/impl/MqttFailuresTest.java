package io.freedriver.autonomy.mqtt.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.ConnectException;
import javax.net.ssl.SSLHandshakeException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.Test;

class MqttFailuresTest {

    @Test
    void namesBadCredentialsAndNotAuthorized() {
        MqttException badCredentials = new MqttException(MqttException.REASON_CODE_FAILED_AUTHENTICATION);
        assertEquals("bad credentials", MqttFailures.describe(badCredentials).cause());
        assertEquals("4", MqttFailures.describe(badCredentials).reasonCode());

        MqttException notAuthorized = new MqttException(MqttException.REASON_CODE_NOT_AUTHORIZED);
        assertEquals("not authorized", MqttFailures.describe(notAuthorized).cause());
        assertEquals("5", MqttFailures.describe(notAuthorized).reasonCode());
    }

    @Test
    void namesCertificateHostnameAndUnreachableCauses() {
        SSLHandshakeException hostname = new SSLHandshakeException(
                "No subject alternative names matching IP address 127.0.0.1 found");
        assertEquals("hostname mismatch", MqttFailures.describe(hostname).cause());

        SSLHandshakeException untrusted = new SSLHandshakeException("PKIX path building failed");
        untrusted.initCause(new FakeCertPathException("unable to find valid certification path"));
        assertEquals("certificate not trusted", MqttFailures.describe(untrusted).cause());

        MqttException wrapped = new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR, untrusted);
        assertEquals("certificate not trusted", MqttFailures.describe(wrapped).cause());

        assertEquals("unreachable or timeout",
                MqttFailures.describe(new ConnectException("Connection refused")).cause());
        assertEquals("password file unreadable",
                MqttFailures.describe(new ConfigFileException("password file unreadable")).cause());
    }

    private static final class FakeCertPathException extends Exception {
        private FakeCertPathException(String message) {
            super(message);
        }
    }
}
