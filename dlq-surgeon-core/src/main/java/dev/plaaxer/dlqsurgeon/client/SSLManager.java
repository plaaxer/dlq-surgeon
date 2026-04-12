package dev.plaaxer.dlqsurgeon.client;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class SSLManager {

    private SSLManager() {}

    /**
     * Builds an {@link SSLContext} from the provided PEM paths.
     *
     * <ul>
     *   <li>{@code caPath} non-null → load a custom CA cert to verify the server (replaces JVM default trust store).</li>
     *   <li>{@code certPath} + {@code keyPath} both non-null → enable mTLS (client presents its own certificate).</li>
     *   <li>All null → returns {@code SSLContext.getDefault()} (JVM trust store, no client cert).</li>
     * </ul>
     *
     * @param caPath   path to a CA certificate PEM, or null to use the JVM default trust store
     * @param certPath path to a client certificate PEM for mTLS, or null
     * @param keyPath  path to a PKCS#8 private key PEM for mTLS, or null
     */
    /**
     * Convenience overload used by clients — returns null (plain connection) when tls=false.
     * Callers treat null as "no TLS" and use plain HTTP/AMQP.
     */
    public static SSLContext build(String caPath, String certPath, String keyPath, boolean tls) throws Exception {
        if (!tls) return null;
        return build(caPath, certPath, keyPath);
    }

    public static SSLContext build(String caPath, String certPath, String keyPath) throws Exception {
        if (caPath == null && certPath == null && keyPath == null) {
            return SSLContext.getDefault();
        }

        TrustManager[] trustManagers = caPath != null
                ? buildTrustManagers(caPath)
                : null; // default trust store

        KeyManager[] keyManagers = (certPath != null && keyPath != null)
                ? buildKeyManagers(certPath, keyPath)
                : null; // one-way

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(keyManagers, trustManagers, null);
        return ctx;
    }

    private static TrustManager[] buildTrustManagers(String caPath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] pemBytes = Files.readAllBytes(Path.of(caPath));
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(pemBytes));

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null); // initialise empty
        trustStore.setCertificateEntry("ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf.getTrustManagers();
    }

    private static KeyManager[] buildKeyManagers(String certPath, String keyPath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] certBytes = Files.readAllBytes(Path.of(certPath));
        Certificate clientCert = cf.generateCertificate(new ByteArrayInputStream(certBytes));

        PrivateKey privateKey = loadPrivateKey(keyPath);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        // empty password: the key is already unencrypted at this point
        keyStore.setKeyEntry("client", privateKey, new char[0], new Certificate[]{clientCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, new char[0]);
        return kmf.getKeyManagers();
    }

    /**
     * Parses a PKCS#8 PEM private key. Tries RSA then EC.
     */
    private static PrivateKey loadPrivateKey(String keyPath) throws Exception {
        String pem = Files.readString(Path.of(keyPath));

        if (pem.contains("BEGIN RSA PRIVATE KEY") || pem.contains("BEGIN EC PRIVATE KEY")) {
            throw new IllegalArgumentException(
                    "PKCS#1 private key format is not supported. Convert to PKCS#8 with:\n" +
                    "  openssl pkcs8 -topk8 -nocrypt -in key.pem -out key-pkcs8.pem");
        }

        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] der = Base64.getDecoder().decode(stripped);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);

        for (String algorithm : new String[]{"RSA", "EC"}) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec);
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException(
                "Could not parse private key — unsupported algorithm (expected RSA or EC).");
    }
}