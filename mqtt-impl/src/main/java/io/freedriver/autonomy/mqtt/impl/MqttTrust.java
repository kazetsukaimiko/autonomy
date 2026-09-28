package io.freedriver.autonomy.mqtt.impl;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * TLS socket factory. With no CA file, the JVM public trust store is used.
 * Hostname checks stay on either way.
 */
final class MqttTrust {

    private MqttTrust() {
    }

    static SSLSocketFactory socketFactory(Optional<Path> caFile) throws Exception {
        SSLSocketFactory factory = caFile.isPresent()
                ? fromCaFile(caFile.get())
                : (SSLSocketFactory) SSLContext.getDefault().getSocketFactory();
        return new HostnameCheckingFactory(factory);
    }

    private static SSLSocketFactory fromCaFile(Path caFile) throws Exception {
        if (!Files.isRegularFile(caFile)) {
            throw new ConfigFileException("ca file unreadable");
        }
        CertificateFactory certificates = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> parsed;
        try (InputStream in = Files.newInputStream(caFile)) {
            parsed = certificates.generateCertificates(in);
        } catch (Exception e) {
            throw new ConfigFileException("ca file unreadable");
        }
        if (parsed.isEmpty()) {
            throw new ConfigFileException("ca file unreadable");
        }
        KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
        trust.load(null, null);
        int index = 0;
        for (Certificate certificate : parsed) {
            trust.setCertificateEntry("ca-" + index++, certificate);
        }
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init(trust);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagers.getTrustManagers(), null);
        return context.getSocketFactory();
    }

    /**
     * Sets HTTPS endpoint identification before the handshake, including on the
     * unconnected socket Paho connects itself.
     */
    private static final class HostnameCheckingFactory extends SSLSocketFactory {

        private final SSLSocketFactory delegate;

        private HostnameCheckingFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        private Socket identify(Socket socket) {
            if (socket instanceof SSLSocket ssl) {
                SSLParameters parameters = ssl.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                ssl.setSSLParameters(parameters);
            }
            return socket;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket() throws java.io.IOException {
            return identify(delegate.createSocket());
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws java.io.IOException {
            return identify(delegate.createSocket(socket, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws java.io.IOException {
            return connect(new InetSocketAddress(host, port), null);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress local, int localPort) throws java.io.IOException {
            return connect(new InetSocketAddress(host, port), new InetSocketAddress(local, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws java.io.IOException {
            return connect(new InetSocketAddress(host, port), null);
        }

        @Override
        public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort) throws java.io.IOException {
            return connect(new InetSocketAddress(host, port), new InetSocketAddress(local, localPort));
        }

        private Socket connect(InetSocketAddress remote, InetSocketAddress local) throws java.io.IOException {
            Socket socket = identify(delegate.createSocket());
            if (local != null) {
                socket.bind(local);
            }
            socket.connect(remote);
            return socket;
        }
    }
}
