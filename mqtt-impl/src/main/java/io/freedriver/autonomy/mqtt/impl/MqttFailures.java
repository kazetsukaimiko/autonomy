package io.freedriver.autonomy.mqtt.impl;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Names a connect or publish failure for the log. Does not include messages that
 * might carry a password, a payload, or a certificate.
 */
public final class MqttFailures {

    private MqttFailures() {
    }

    public record Description(String cause, String reasonCode, String causeClass) {
    }

    public static Description describe(Throwable error) {
        if (error == null) {
            return new Description("unreachable or timeout", "none", "none");
        }
        Integer mqttCode = null;
        boolean hostname = false;
        boolean untrusted = false;
        boolean unreachable = false;
        String fileCause = null;
        Throwable cursor = error;
        while (cursor != null) {
            if (cursor instanceof ConfigFileException file) {
                fileCause = file.getMessage();
            }
            if (cursor instanceof MqttException mqtt) {
                int reason = mqtt.getReasonCode();
                if (reason == MqttException.REASON_CODE_FAILED_AUTHENTICATION) {
                    return described("bad credentials", reason, error);
                }
                if (reason == MqttException.REASON_CODE_NOT_AUTHORIZED) {
                    return described("not authorized", reason, error);
                }
                if (reason != 0) {
                    mqttCode = reason;
                }
            }
            String message = cursor.getMessage() == null
                    ? ""
                    : cursor.getMessage().toLowerCase(Locale.ROOT);
            String type = cursor.getClass().getName();
            if (message.contains("no subject alternative")
                    || message.contains("no name matching")
                    || message.contains("hostname")
                    || type.endsWith("SSLPeerUnverifiedException")) {
                hostname = true;
            }
            if (type.contains("CertPath")
                    || type.endsWith("CertificateException")
                    || message.contains("pkix")
                    || message.contains("certpath")) {
                untrusted = true;
            }
            if (cursor instanceof ConnectException
                    || cursor instanceof UnknownHostException
                    || cursor instanceof SocketTimeoutException
                    || cursor instanceof NoRouteToHostException
                    || message.contains("connection refused")
                    || message.contains("timed out")
                    || message.contains("timeout")) {
                unreachable = true;
            }
            Throwable next = cursor.getCause();
            cursor = next == cursor ? null : next;
        }
        if (fileCause != null) {
            return new Description(fileCause, "none", error.getClass().getName());
        }
        if (hostname) {
            return described("hostname mismatch", mqttCode, error);
        }
        if (untrusted) {
            return described("certificate not trusted", mqttCode, error);
        }
        if (unreachable) {
            return described("unreachable or timeout", mqttCode, error);
        }
        return described("unreachable or timeout", mqttCode, error);
    }

    private static Description described(String cause, Integer reasonCode, Throwable error) {
        String code = reasonCode == null ? "none" : Integer.toString(reasonCode);
        return new Description(cause, code, error.getClass().getName());
    }
}
